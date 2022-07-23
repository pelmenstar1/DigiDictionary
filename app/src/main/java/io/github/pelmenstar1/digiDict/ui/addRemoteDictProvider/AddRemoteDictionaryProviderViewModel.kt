package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.util.Log
import android.util.Patterns
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val _dbErrorFlow = MutableStateFlow<AddRemoteDictionaryProviderMessage?>(null)

    private val checkValueChannel = Channel<Message>(capacity = Channel.UNLIMITED)
    private var isCheckValueJobStarted = false

    val nameErrorFlow = _nameErrorFlow.asStateFlow()
    val schemaErrorFlow = _schemaErrorFlow.asStateFlow()
    val dbErrorFlow = _dbErrorFlow.asStateFlow()

    var onSuccessfulAddition: (() -> Unit)? = null

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

    @MainThread
    private fun scheduleCheckValue(type: Int, value: String) {
        if (value.isBlank()) {
            val flow = when (type) {
                TYPE_NAME -> _nameErrorFlow
                TYPE_SCHEMA -> _schemaErrorFlow
                else -> throw RuntimeException()
            }

            flow.value = AddRemoteDictionaryProviderMessage.EMPTY_TEXT
        } else {
            if (!isCheckValueJobStarted) {
                isCheckValueJobStarted = true

                startCheckValueJob()
            }

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

                withContext(Dispatchers.Main) {
                    onSuccessfulAddition?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "during addition", e)
            }
        }
    }

    private fun startCheckValueJob() {
        viewModelScope.launch {
            val allProviders = remoteDictProviderDao.getAll()

            while (isActive) {
                val (type, value) = checkValueChannel.receive()

                when (type) {
                    TYPE_NAME -> {
                        _nameErrorFlow.value = AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS.takeIf {
                            allProviders.any { it.name == value }
                        }
                    }
                    TYPE_SCHEMA -> {
                        _schemaErrorFlow.value = when {
                            !Patterns.WEB_URL.matcher(value)
                                .matches() -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL
                            !value.contains("\$query$") -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER
                            allProviders.any { it.schema == value } -> AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS
                            else -> null
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
    }
}