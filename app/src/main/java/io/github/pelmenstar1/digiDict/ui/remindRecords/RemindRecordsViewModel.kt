package io.github.pelmenstar1.digiDict.ui.remindRecords

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RemindRecordsViewModel @Inject constructor(
    private val recordDao: RecordDao,
    appPreferences: AppPreferences
) : SingleDataLoadStateViewModel<Array<Record>>(TAG) {
    private val random = Random(System.currentTimeMillis())

    private val preferencesSnapshotFlow = appPreferences.getSnapshotFlow()

    val showMeaningFlow = preferencesSnapshotFlow.map { it.remindShowMeaning }

    override fun DataLoadStateManager.FlowBuilder<Array<Record>>.buildDataFlow() = fromFlow {
        preferencesSnapshotFlow.map {
            recordDao.getRandomRecordsRegardlessScore(random, it.remindItemsSize)
        }
    }

    companion object {
        private const val TAG = "RemindRecordsVM"
    }
}