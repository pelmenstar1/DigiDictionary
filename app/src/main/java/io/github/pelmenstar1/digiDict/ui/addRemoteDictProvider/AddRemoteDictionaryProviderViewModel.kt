package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class AddRemoteDictionaryProviderViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private data class Message(val type: Int, val value: String)

    private val checkValueChannel = Channel<Message>(capacity = Channel.UNLIMITED)
    private val isCheckValueJobStarted = AtomicBoolean()

    private val _nameErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _schemaErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _isInputsEnabled = MutableStateFlow(true)

    val nameErrorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>
        get() = _nameErrorFlow

    val schemaErrorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>
        get() = _schemaErrorFlow

    val isInputEnabledFlow: StateFlow<Boolean>
        get() = _isInputsEnabled

    val validityFlow = ValidityFlow(validityScheme)

    val addAction = viewModelAction(TAG) {
        val newProvider = RemoteDictionaryProviderInfo(
            name = name.trim(),
            schema = schema.trim(),
            urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules(spaceReplacement)
        )

        remoteDictProviderDao.insert(newProvider)
    }

    val validityCheckErrorFlow = MutableSharedFlow<Throwable?>(replay = 1)

    val nameFlow = savedStateHandle.getStateFlow(KEY_NAME, "")
    val schemaFlow = savedStateHandle.getStateFlow(KEY_SCHEMA, "")
    val spaceReplacementFlow = savedStateHandle.getStateFlow(
        KEY_SPACE_REPLACEMENT,
        RemoteDictionaryProviderInfo.UrlEncodingRules.DEFAULT_SPACE_REPLACEMENT
    )

    /**
     * Name of the provider.
     */
    var name: String
        get() = nameFlow.value
        set(value) {
            if (nameFlow.value != value) {
                savedStateHandle[KEY_NAME] = value
                scheduleCheckValue(TYPE_NAME, value)
            }
        }

    /**
     * Schema of the provider.
     */
    var schema: String
        get() = schemaFlow.value
        set(value) {
            if (schemaFlow.value != value) {
                savedStateHandle[KEY_SCHEMA] = value
                scheduleCheckValue(TYPE_SCHEMA, value)
            }
        }

    var spaceReplacement: Char
        get() = spaceReplacementFlow.value
        set(value) {
            savedStateHandle[KEY_SPACE_REPLACEMENT] = value
        }

    init {
        startCheckValueJob()
    }

    fun add() {
        addAction.runWhenValid(validityFlow)
    }

    fun restartValidityCheck() {
        validityFlow.mutate {
            disable(nameValidityField, isComputed = false)
            disable(schemaValidityField, isComputed = false)
        }

        startCheckValueJobIfNecessary()
        checkValueChannel.run {
            // trySend() will always succeed as the channel is unlimited.
            trySend(Message(TYPE_NAME, name))
            trySend(Message(TYPE_SCHEMA, schema))
        }
    }

    private fun scheduleCheckValue(type: Int, value: String) {
        validityFlow.mutate {
            disable(getValidityField(type), isComputed = false)
        }

        startCheckValueJobIfNecessary()

        // trySend() will always succeed as the channel is unlimited.
        checkValueChannel.trySend(Message(type, value))
    }

    private fun startCheckValueJobIfNecessary() {
        if (isCheckValueJobStarted.compareAndSet(false, true)) {
            startCheckValueJob()
        }
    }

    private fun startCheckValueJob() {
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

                // Disable all validity fields in order to disable "Add" button
                validityFlow.mutate {
                    disable(nameValidityField, isComputed = true)
                    disable(schemaValidityField, isComputed = true)
                }

                if (e !is CancellationException) {
                    validityCheckErrorFlow.emit(e)
                }

                return@launch
            }

            // If the job is started after the error, _isNameEnabledFlow's and _isSchemaEnabledFlow's values might be false.
            // So after we know allProviders are loaded successfully, we can re-enable inputs.
            _isInputsEnabled.value = true

            while (isActive) {
                val message = checkValueChannel.receive()
                val type = message.type
                val value = message.value.trim()

                val errorFlow = when (type) {
                    TYPE_NAME -> _nameErrorFlow
                    TYPE_SCHEMA -> _schemaErrorFlow
                    else -> throw IllegalArgumentException("type")
                }

                val error = if (value.isEmpty()) {
                    AddRemoteDictionaryProviderMessage.EMPTY_TEXT
                } else {
                    when (type) {
                        TYPE_NAME -> {
                            if (allProviders.any { it.name == value }) {
                                AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS
                            } else {
                                null
                            }
                        }
                        TYPE_SCHEMA -> {
                            when {
                                !isValidUrl(value) -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL
                                !value.contains("\$query$") -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER
                                allProviders.any { it.schema == value } -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS
                                else -> null
                            }
                        }
                        else -> throw IllegalStateException("Invalid message type")
                    }
                }

                errorFlow.value = error
                validityFlow.mutate {
                    set(getValidityField(type), error == null, isComputed = true)
                }
            }
        }
    }


    companion object {
        private const val TAG = "AddRDP_VM"

        private const val KEY_NAME = "io.github.pelmenstar1.digiDict.AddRemoteDictionaryProviderVM.name"
        private const val KEY_SCHEMA = "io.github.pelmenstar1.digiDict.AddRemoteDictionaryProviderVM.schema"
        private const val KEY_SPACE_REPLACEMENT =
            "io.github.pelmenstar1.digiDict.AddRemoteDictionaryProviderVM.spaceReplacement"

        private const val TYPE_NAME = 0
        private const val TYPE_SCHEMA = 1

        private val urlPattern =
            Pattern.compile("^https?://(?:www\\.)?[-a-zA-Z\\d@:%._+~#=]{1,256}\\.[a-zA-Z\\d()]{1,6}\\b([-a-zA-Z\\d()@:%_+.~#?&/=]|(\\\$query\\\$))*\$")

        val nameValidityField = ValidityFlow.Field(ordinal = 0)
        val schemaValidityField = ValidityFlow.Field(ordinal = 1)

        private val validityScheme = ValidityFlow.Scheme(nameValidityField, schemaValidityField)

        internal fun isValidUrl(value: String): Boolean {
            return urlPattern.matcher(value).matches()
        }

        internal fun getValidityField(type: Int) = when (type) {
            TYPE_NAME -> nameValidityField
            TYPE_SCHEMA -> schemaValidityField
            else -> throw IllegalArgumentException("type")
        }
    }
}