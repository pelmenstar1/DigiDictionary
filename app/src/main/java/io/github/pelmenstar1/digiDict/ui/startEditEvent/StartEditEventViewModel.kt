package io.github.pelmenstar1.digiDict.ui.startEditEvent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
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
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val currentEventIdFlow = MutableStateFlow<Int?>(null)

    private var isCheckNameJobStarted = false
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
                if (name.isBlank()) {
                    _nameErrorFlow.value = StartEditEventError.EMPTY_TEXT
                }
            }

            startCheckEventNameJob()
        }

    private val currentEventStateManager = DataLoadStateManager<EventInfo>(TAG)
    val currentEventStateFlow = currentEventStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentEventIdFlow.filterNotNull().map { id ->
                val currentEvent = eventDao.getById(id)!!

                setNameInternal(currentEvent.name)

                validity.mutate {
                    set(nameValidityField, true)
                }

                currentEvent
            }
        }
    }

    val nameFlow = savedStateHandle.getStateFlow(KEY_NAME, "")

    var name: String
        get() = nameFlow.value
        set(value) {
            if (nameFlow.value != value) {
                savedStateHandle[KEY_NAME] = value

                validity.mutate {
                    set(nameValidityField, value = false, isComputed = false)
                }

                checkEventNameChannel.trySendBlocking(value)
            }
        }

    val startOrEditAction = viewModelAction(TAG) {
        val curEventId = currentEventId
        val trimmedName = name.trim()

        if (curEventId >= 0) {
            val currentEvent = currentEventStateFlow.firstSuccess()
            val updatedEvent = EventInfo(
                id = curEventId,
                name = trimmedName,
                currentEvent.startEpochSeconds, currentEvent.endEpochSeconds
            )

            eventDao.update(updatedEvent)
        } else {
            val nowEpochSeconds = currentEpochSecondsProvider.get { Utc }

            val newEvent = EventInfo(
                id = 0,
                name = trimmedName,
                startEpochSeconds = nowEpochSeconds,
                endEpochSeconds = -1
            )

            eventDao.insert(newEvent)
        }
    }

    private fun setNameInternal(value: String) {
        savedStateHandle[KEY_NAME] = value
    }

    private fun startCheckEventNameJob() {
        if (!isCheckNameJobStarted) {
            isCheckNameJobStarted = true

            viewModelScope.launch(Dispatchers.Default) {
                val names = eventDao.getAllNames()
                val currentName = if (currentEventId >= 0) {
                    currentEventStateFlow.firstSuccess().name
                } else {
                    null
                }

                while (isActive) {
                    val name = checkEventNameChannel.receive().trim()
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
    }

    fun startOrEdit() {
        startOrEditAction.runWhenValid(validity)
    }

    fun retryLoadCurrentEvent() {
        currentEventStateManager.retry()
    }

    companion object {
        private const val TAG = "StartEditEventViewModel"

        private const val KEY_NAME = "io.github.pelmenstar1.StartEditEventViewModel.name"

        private val nameValidityField = ValidityFlow.Field(ordinal = 0)
        private val validityScheme = ValidityFlow.Scheme(nameValidityField)
    }
}