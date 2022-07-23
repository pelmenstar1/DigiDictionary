package io.github.pelmenstar1.digiDict.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.SECONDS_IN_WEEK
import io.github.pelmenstar1.digiDict.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    val countFlow = recordDao.countFlow()

    private val _additionStatsFlow = MutableStateFlow<AdditionStats?>(null)
    val additionStatsFlow = _additionStatsFlow.asStateFlow()

    init {
        computeStats()
    }

    private fun computeStats() {
        viewModelScope.launch(Dispatchers.Default) {
            val nowEpochSecondsUtc = System.currentTimeMillis() / 1000

            // We don't need dateTimes before last 31 days.
            val dateTimes = recordDao.getAllDateTimesOrderByDescAfter(nowEpochSecondsUtc - 31 * SECONDS_IN_DAY)

            var recordsAddedLast24Hours = 0
            var recordsAddedLast7Days = 0
            var recordsAddedLast31Days = 0

            for (epochSeconds in dateTimes) {
                val delta = nowEpochSecondsUtc - epochSeconds

                if (delta <= SECONDS_IN_DAY) {
                    recordsAddedLast24Hours++
                }

                if (delta <= SECONDS_IN_WEEK) {
                    recordsAddedLast7Days++
                }

                if (delta <= 31 * SECONDS_IN_DAY) {
                    recordsAddedLast31Days++
                }
            }

            _additionStatsFlow.value = AdditionStats(
                recordsAddedLast24Hours,
                recordsAddedLast7Days,
                recordsAddedLast31Days
            )
        }
    }
}