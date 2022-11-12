package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.ExclusiveWaitHandleForFlowCondition
import io.github.pelmenstar1.digiDict.common.viewModelAction
import io.github.pelmenstar1.digiDict.common.withBit
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AddRemoteDictionaryProviderViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao
) : ViewModel() {
    private data class Message(val type: Int, val value: String)

    private val _nameErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _schemaErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _isInputsEnabled = MutableStateFlow(true)

    // As name and schema is empty by default,
    // they're invalid but they're computed (the state won't be changed for particular name and schema)
    internal val _validityFlow = MutableStateFlow(NAME_COMPUTED_VALIDITY_BIT or SCHEMA_COMPUTED_VALIDITY_BIT)

    private val checkValueChannel = Channel<Message>(capacity = Channel.UNLIMITED)
    private val isCheckValueJobStarted = AtomicBoolean()

    private val addWaitHandle = ExclusiveWaitHandleForFlowCondition(
        viewModelScope,
        _validityFlow,
        stopExclusiveCondition = { (it and COMPUTED_MASK) == COMPUTED_MASK },
        runActionCondition = { it == ALL_VALID_MASK },
        action = { addAction.run() }
    )

    val nameErrorFlow = _nameErrorFlow.asStateFlow()
    val schemaErrorFlow = _schemaErrorFlow.asStateFlow()
    val isInputEnabledFlow = _isInputsEnabled.asStateFlow()
    val validityFlow = _validityFlow.asStateFlow()

    val addAction = viewModelAction(TAG) {
        val newProvider = RemoteDictionaryProviderInfo(
            name = name,
            schema = schema,
            urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules(spaceReplacement)
        )

        remoteDictProviderDao.insert(newProvider)
    }

    val validityCheckErrorFlow = MutableSharedFlow<Throwable?>(replay = 1)

    /**
     * Name of the provider, the string is expected to be without leading and trailing whitespaces
     */
    var name: String = ""
        set(value) {
            field = value
            scheduleCheckValue(TYPE_NAME, value)
        }

    /**
     * Schema of the provider, the string is expected to be without leading and trailing whitespaces
     */
    var schema: String = ""
        set(value) {
            field = value
            scheduleCheckValue(TYPE_SCHEMA, value)
        }

    var spaceReplacement: Char = RemoteDictionaryProviderInfo.UrlEncodingRules.DEFAULT_SPACE_REPLACEMENT

    fun restartValidityCheck() {
        scheduleCheckValue(TYPE_NAME, name)
        scheduleCheckValue(TYPE_SCHEMA, schema)
    }

    private fun scheduleCheckValue(type: Int, value: String) {
        _validityFlow.update {
            it.withBit(valueValidityMask(type), false)
                .withBit(valueValidityComputedMask(type), false)
        }

        startCheckValueJobIfNecessary()
        checkValueChannel.trySendBlocking(Message(type, value))
    }

    fun add() {
        val validity = _validityFlow.value

        if (validity == ALL_VALID_MASK) {
            addAction.run()
        } else if ((validity and COMPUTED_MASK) != COMPUTED_MASK) {
            addWaitHandle.runAction()
        }
    }

    private fun startCheckValueJobIfNecessary() {
        if (isCheckValueJobStarted.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.Default) {
                validityCheckErrorFlow.emit(null)
                val allProviders: Array<RemoteDictionaryProviderInfo>

                try {
                    allProviders = remoteDictProviderDao.getAll()
                } catch (e: Exception) {
                    // The job still can be restarted.
                    isCheckValueJobStarted.set(false)
                    _isInputsEnabled.value = false

                    // There can be no errors as name and schema inputs are disabled.
                    _nameErrorFlow.value = null
                    _schemaErrorFlow.value = null

                    // Unset all validity bits in order to disable "Add" button
                    _validityFlow.value = 0

                    if (e !is CancellationException) {
                        validityCheckErrorFlow.emit(e)
                    }

                    return@launch
                }

                // If the job is started after the error, _isNameEnabledFlow's and _isSchemaEnabledFlow's values might be false.
                // So after we know allProviders are loaded successfully, we can re-enable inputs.
                _isInputsEnabled.value = true

                while (isActive) {
                    val (type, value) = checkValueChannel.receive()

                    val errorFlow = when (type) {
                        TYPE_NAME -> _nameErrorFlow
                        TYPE_SCHEMA -> _schemaErrorFlow
                        else -> throw IllegalArgumentException("type")
                    }

                    var error: AddRemoteDictionaryProviderMessage? = null

                    if (value.isEmpty()) {
                        error = AddRemoteDictionaryProviderMessage.EMPTY_TEXT
                    } else {
                        when (type) {
                            TYPE_NAME -> {
                                error = AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS.takeIf {
                                    allProviders.any { it.name == value }
                                }
                            }
                            TYPE_SCHEMA -> {
                                error = when {
                                    !Patterns.WEB_URL.matcher(value).matches() ->
                                        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL

                                    !value.contains("\$query$") ->
                                        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER

                                    allProviders.any { it.schema == value } ->
                                        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS

                                    else -> null
                                }
                            }
                        }
                    }

                    errorFlow.value = error
                    _validityFlow.update {
                        it.withBit(valueValidityMask(type), error == null)
                            .withBit(valueValidityComputedMask(type), true)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AddRDP_VM"

        private const val TYPE_COUNT = 2
        private const val TYPE_NAME = 0
        private const val TYPE_SCHEMA = 1

        const val NAME_VALIDITY_BIT = 1 shl TYPE_NAME
        const val SCHEMA_VALIDITY_BIT = 1 shl TYPE_SCHEMA

        const val NAME_COMPUTED_VALIDITY_BIT = 1 shl (TYPE_NAME + TYPE_COUNT)
        const val SCHEMA_COMPUTED_VALIDITY_BIT = 1 shl (TYPE_SCHEMA + TYPE_COUNT)

        const val COMPUTED_MASK = NAME_COMPUTED_VALIDITY_BIT or SCHEMA_COMPUTED_VALIDITY_BIT
        const val ALL_VALID_MASK = 0xF

        fun valueValidityMask(type: Int) = 1 shl type
        fun valueValidityComputedMask(type: Int) = 1 shl (type + TYPE_COUNT)
    }
}