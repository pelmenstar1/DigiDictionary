package io.github.pelmenstar1.digiDict.ui.startEditEvent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.common.trimToString
import io.github.pelmenstar1.digiDict.data.EventDao
import io.github.pelmenstar1.digiDict.data.EventInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartEditEventViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : ViewModel() {
    private val currentEventIdFlow = MutableStateFlow<Int?>(null)
    private val checkEventNameChannel = Channel<String>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _nameErrorFlow = MutableStateFlow<StartEditEventError?>(null)

    val nameErrorFlow: Flow<StartEditEventError?>
        get() = _nameErrorFlow

    val validity = ValidityFlow(validityScheme)

    var currentEventId: Int = -1
        set(value) {
            field = value

            if (value >= 0) {
                currentEventIdFlow.value = value
            } else {
                validity.mutate {
                    set(nameValidityField, true)
                }

                _nameErrorFlow.value = StartEditEventError.EMPTY_TEXT
                startCheckEventNameJob()
            }
        }

    private val currentEventStateManager = DataLoadStateManager<EventInfo>(TAG)
    val currentEventStateFlow = currentEventStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentEventIdFlow.filterNotNull().map { id ->
                val currentEvent = eventDao.getById(id)!!

                validity.mutate {
                    set(nameValidityField, true)
                }

                startCheckEventNameJob()

                currentEvent
            }
        }
    }

    private var _name: String = ""
    var name: CharSequence
        get() = _name
        set(value) {
            val trimmed = value.trimToString()
            _name = trimmed

            validity.mutate {
                set(nameValidityField, value = false, isComputed = false)
            }

            checkEventNameChannel.trySendBlocking(trimmed)
        }

    val startOrEditAction = viewModelAction(TAG) {
        val curEventId = currentEventId

        if (curEventId >= 0) {
            val currentEvent = currentEventStateFlow.firstSuccess()
            val updatedEvent = EventInfo(
                id = curEventId,
                name = _name,
                currentEvent.startEpochSeconds, currentEvent.endEpochSeconds
            )

            eventDao.update(updatedEvent)
        } else {
            val nowEpochSeconds = currentEpochSecondsProvider.get { Utc }

            val newEvent = EventInfo(
                id = 0,
                name = _name,
                startEpochSeconds = nowEpochSeconds,
                endEpochSeconds = -1
            )

            eventDao.insert(newEvent)
        }
    }

    private fun startCheckEventNameJob() {
        viewModelScope.launch(Dispatchers.Default) {
            val names = eventDao.getAllNames()
            val currentName = if (currentEventId >= 0) {
                currentEventStateFlow.firstSuccess().name
            } else {
                null
            }

            while (isActive) {
                val name = checkEventNameChannel.receive()
                val error = when {
                    name.isEmpty() -> StartEditEventError.EMPTY_TEXT
                    name != currentName && names.contains(name) -> StartEditEventError.NAME_EXISTS
                    else -> null
                }

                val isValid = error == null

                validity.mutate {
                    set(nameValidityField, isValid, isComputed = true)
                }
                _nameErrorFlow.value = error
            }
        }
    }

    fun startOrEdit() {
        startOrEditAction.runWhenValid(validity)
    }

    fun retryLoadCurrentEvent() {
        currentEventStateManager.retry()
    }

    companion object {
        private val nameValidityField = ValidityFlow.Field(ordinal = 0)
        private val validityScheme = ValidityFlow.Scheme(nameValidityField)

        private const val TAG = "StartEditEventViewModel"
    }
}