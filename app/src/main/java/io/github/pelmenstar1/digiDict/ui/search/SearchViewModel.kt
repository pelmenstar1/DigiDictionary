package io.github.pelmenstar1.digiDict.ui.search

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private data class Request(
        val query: String,
        val updateRecords: Boolean = false,
        val forceRepeatRequest: Boolean = false
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

                sendRequest(Request(str))
            }
        }

    private val requestFlow = MutableStateFlow<Request?>(null)
    private val isSearchJobStarted = AtomicBoolean()

    private val _result = MutableStateFlow(FilteredArray.empty<Record>())
    val result = _result.asStateFlow()

    var onError: (() -> Unit)? = null

    init {
        appDatabase.addRecordTableObserver(this) {
            sendRequest(Request(_query, updateRecords = true))
        }
    }

    fun repeatSearchQuery() {
        sendRequest(Request(_query, forceRepeatRequest = true))
    }

    private fun sendRequest(request: Request) {
        startSearchJobIfNecessary()

        requestFlow.value = request
    }

    private fun startSearchJobIfNecessary() {
        if (isSearchJobStarted.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    var records: Array<Record>? = null

                    requestFlow.filterNotNull().collect { (query, updateRecords) ->
                        if (records == null || updateRecords) {
                            records = recordDao.getAllRecordsOrderByDateTime()
                        }

                        _result.value = if (query.isBlank()) {
                            FilteredArray.empty()
                        } else {
                            records!!.filterFast { (_, expression, rawMeaning) ->
                                expression.startsWith(query, ignoreCase = true) ||
                                        ComplexMeaning.anyElementStartsWith(rawMeaning, query, ignoreCase = true)
                            }
                        }
                    }
                } catch (e: Exception) {
                    onError?.invoke()

                    isSearchJobStarted.set(false)
                }
            }
        }
    }
}