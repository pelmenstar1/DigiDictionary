package io.github.pelmenstar1.digiDict.ui.remindRecords

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.getFlow
import io.github.pelmenstar1.digiDict.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RemindRecordsViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val appPreferences: AppPreferences
) : SingleDataLoadStateViewModel<Array<Record>>(TAG) {
    private val random = Random(System.currentTimeMillis())

    override fun DataLoadStateManager.FlowBuilder<Array<Record>>.buildDataFlow() = fromFlow {
        appPreferences.getFlow { remindItemsSize }.map { size ->
            recordDao.getRandomRecordsRegardlessScore(random, size)
        }
    }

    companion object {
        private const val TAG = "RemindRecordsVM"
    }
}