package io.github.pelmenstar1.digiDict.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SupportSQLiteStatement
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    private val _resultFlow = MutableStateFlow<CommonStats?>(null)
    val resultFlow = _resultFlow.asStateFlow()

    val onLoadError = Event()

    init {
        computeStats()
    }

    fun computeStats() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val count = recordDao.count()
                val additionStats = computeAdditionStats()

                _resultFlow.value = CommonStats(
                    count, additionStats
                )
            } catch (e: Exception) {
                Log.e(TAG, "during compute()", e)

                onLoadError.raiseOnMainThread()
            }
        }
    }

    private fun SupportSQLiteStatement.bindLongAndExecuteToInt(value: Long): Int {
        bindLong(1, value)

        return simpleQueryForLong().toInt()
    }

    private fun computeAdditionStats(): AdditionStats {
        val db = appDatabase
        val nowEpochSecondsUtc = System.currentTimeMillis() / 1000L

        val statement = db.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ?")

        val last24Hours = statement.bindLongAndExecuteToInt(nowEpochSecondsUtc - SECONDS_IN_DAY)
        val last7Days = statement.bindLongAndExecuteToInt(nowEpochSecondsUtc - 7 * SECONDS_IN_DAY)
        val last31Days = statement.bindLongAndExecuteToInt(nowEpochSecondsUtc - 31 * SECONDS_IN_DAY)

        return AdditionStats(last24Hours, last7Days, last31Days)
    }

    companion object {
        private const val TAG = "StatsViewModel"
    }
}