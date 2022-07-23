package io.github.pelmenstar1.digiDict.ui.search

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.utils.FilteredArray
import io.github.pelmenstar1.digiDict.utils.filterFast
import io.github.pelmenstar1.digiDict.utils.trimToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private data class Command(
        val query: String,
        val updateRecords: Boolean = false
    )

    private val recordDao = appDatabase.recordDao()

    // query is changed only in UI code (main-thread),
    // so it's safe to use them when we know we are on the main-thread
    private var _query = ""
    var query: CharSequence
        get() = _query
        set(value) {
            if (_query != value) {
                val str = value.trimToString()
                _query = str

                sendCommand(Command(str))
            }
        }

    private val commandChannel = Channel<Command>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var isSearchJobStarted = false

    private val _result = MutableStateFlow(FilteredArray.empty<Record>())
    val result = _result.asStateFlow()

    init {
        appDatabase.addRecordTableObserver(this) {
            sendCommand(Command(_query, updateRecords = true))
        }
    }

    @MainThread
    private fun sendCommand(value: Command) {
        if (!isSearchJobStarted) {
            isSearchJobStarted = true

            startSearchJob()
        }

        commandChannel.trySend(value)
    }

    private fun startSearchJob() {
        viewModelScope.launch(Dispatchers.Default) {
            var records: Array<Record>? = null

            while (isActive) {
                val command = commandChannel.receive()

                if (records == null || command.updateRecords) {
                    records = recordDao.getAllRecordsOrderByDateTime()
                }

                val query = command.query

                _result.value = if (query.isBlank()) {
                    FilteredArray.empty()
                } else {
                    records.filterFast { (_, expression, rawMeaning) ->
                        expression.startsWith(query, ignoreCase = true) ||
                                ComplexMeaning.anyElementStartsWith(rawMeaning, query, ignoreCase = true)
                    }
                }
            }
        }
    }
}