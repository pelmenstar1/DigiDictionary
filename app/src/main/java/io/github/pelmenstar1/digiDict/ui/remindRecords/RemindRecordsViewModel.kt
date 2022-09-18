package io.github.pelmenstar1.digiDict.ui.remindRecords

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RemindRecordsViewModel @Inject constructor(
    private val recordDao: RecordDao,
    appPreferences: DigiDictAppPreferences
) : SingleDataLoadStateViewModel<Array<ConciseRecordWithBadges>>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = true

    private val random = Random(System.currentTimeMillis())

    private val preferencesSnapshotFlow = appPreferences.getSnapshotFlow()
    val showMeaningFlow = preferencesSnapshotFlow.map { it.remindShowMeaning }

    override fun DataLoadStateManager.FlowBuilder<Array<ConciseRecordWithBadges>>.buildDataFlow() = fromFlow {
        preferencesSnapshotFlow.map {
            recordDao.getRandomConciseRecordsWithBadgesRegardlessScore(random, it.remindItemsSize)
        }
    }

    companion object {
        private const val TAG = "RemindRecordsVM"
    }
}