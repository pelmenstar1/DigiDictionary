package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.util.Log
import android.util.Patterns
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.utils.withBit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AddRemoteDictionaryProviderViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private data class Message(val type: Int, val value: String)

    private val remoteDictProviderDao = appDatabase.remoteDictionaryProviderDao()

    private val _nameErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _schemaErrorFlow =
        MutableStateFlow<AddRemoteDictionaryProviderMessage?>(AddRemoteDictionaryProviderMessage.EMPTY_TEXT)

    private val _isNameEnabledFlow = MutableStateFlow(true)
    private val _isSchemaEnabledFlow = MutableStateFlow(true)

    // As name and schema is empty by default, it's 0 as no validity bits are set.
    private val _validityFlow = MutableStateFlow(0)

    private val checkValueChannel = Channel<Message>(capacity = Channel.UNLIMITED)
    private val isCheckValueJobStarted = AtomicBoolean()

    val nameErrorFlow = _nameErrorFlow.asStateFlow()
    val schemaErrorFlow = _schemaErrorFlow.asStateFlow()
    val isNameEnabledFlow = _isNameEnabledFlow.asStateFlow()
    val isSchemaEnabledFlow = _isNameEnabledFlow.asStateFlow()
    val validityErrorFlow = _validityFlow.asStateFlow()

    val onAdditionError = Event()
    val onValidityCheckError = Event()
    val onSuccessfulAddition = Event()

    var name: String = ""
        set(value) {
            field = value

            scheduleCheckValue(TYPE_NAME, value)
        }

    var schema: String = ""
        set(value) {
            field = value

            scheduleCheckValue(TYPE_SCHEMA, value)
        }

    fun restartValidityCheck() {
        scheduleCheckValue(TYPE_NAME, name)
        scheduleCheckValue(TYPE_SCHEMA, schema)
    }

    @MainThread
    private fun scheduleCheckValue(type: Int, value: String) {
        // During the check, presume the value is invalid. If the value is actually valid, validity bit will be set later.
        _validityFlow.withBit(1 shl type, false)

        if (value.isBlank()) {
            val flow = when (type) {
                TYPE_NAME -> _nameErrorFlow
                TYPE_SCHEMA -> _schemaErrorFlow
                else -> throw RuntimeException()
            }

            flow.value = AddRemoteDictionaryProviderMessage.EMPTY_TEXT
        } else {
            startCheckValueJobIfNecessary()

            checkValueChannel.trySendBlocking(Message(type, value))
        }
    }

    fun add() {
        viewModelScope.launch(Dispatchers.IO) {
            val newProvider = RemoteDictionaryProviderInfo(
                name = name,
                schema = schema
            )

            try {
                remoteDictProviderDao.insert(newProvider)

                onSuccessfulAddition.raiseOnMainThread()
            } catch (e: Exception) {
                Log.e(TAG, "during addition", e)

                onAdditionError.raiseOnMainThread()
            }
        }
    }

    private fun startCheckValueJobIfNecessary() {
        if (isCheckValueJobStarted.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.Default) {
                val allProviders: Array<RemoteDictionaryProviderInfo>

                try {
                    allProviders = remoteDictProviderDao.getAll()
                } catch (e: Exception) {
                    // The job still can be restarted.
                    isCheckValueJobStarted.set(false)

                    _isNameEnabledFlow.value = false
                    _isSchemaEnabledFlow.value = false

                    // There can be no errors as name and schema inputs are disabled.
                    _nameErrorFlow.value = null
                    _schemaErrorFlow.value = null

                    // Unset all validity bits in order to disable "Add" button
                    _validityFlow.value = 0

                    onValidityCheckError.raise()

                    return@launch
                }

                // If the job is started after the error, _isNameEnabledFlow's and _isSchemaEnabledFlow's values might be false.
                // So after we know allProviders are loaded successfully, we can set values to true.
                _isNameEnabledFlow.value = true
                _isSchemaEnabledFlow.value = true

                while (isActive) {
                    val (type, value) = checkValueChannel.receive()

                    when (type) {
                        TYPE_NAME -> {
                            val error = AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS.takeIf {
                                allProviders.any { it.name == value }
                            }

                            _nameErrorFlow.value = error
                            _validityFlow.withBit(NAME_VALIDITY_BIT, error == null)
                        }
                        TYPE_SCHEMA -> {
                            val error = when {
                                !Patterns.WEB_URL.matcher(value).matches() ->
                                    AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL

                                !value.contains("\$query$") ->
                                    AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER

                                allProviders.any { it.schema == value } ->
                                    AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS

                                else -> null
                            }

                            _schemaErrorFlow.value = error
                            _validityFlow.withBit(SCHEMA_VALIDITY_BIT, error == null)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AddRDP_VM"

        private const val TYPE_NAME = 0
        private const val TYPE_SCHEMA = 1

        const val NAME_VALIDITY_BIT = 1 shl TYPE_NAME
        const val SCHEMA_VALIDITY_BIT = 1 shl TYPE_SCHEMA
        const val ALL_VALID_MASK = NAME_VALIDITY_BIT or SCHEMA_VALIDITY_BIT
    }
}