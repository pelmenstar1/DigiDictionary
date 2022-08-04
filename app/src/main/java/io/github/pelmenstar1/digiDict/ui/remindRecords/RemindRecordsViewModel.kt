package io.github.pelmenstar1.digiDict.ui.remindRecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.getFlow
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RemindRecordsViewModel @Inject constructor(
    recordDao: RecordDao,
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val random = Random(System.currentTimeMillis())

    private val resultStateManager = DataLoadStateManager<Array<Record>>(TAG)
    val resultStateFlow = resultStateManager.buildFlow(viewModelScope) {
        fromFlow {
            appPreferences.getFlow { remindItemsSize }.map { size ->
                recordDao.getRandomRecordsRegardlessScore(random, size)
            }
        }
    }

    fun retryLoadResult() {
        resultStateManager.retry()
    }

    companion object {
        private const val TAG = "RemindRecordsVM"
    }
}