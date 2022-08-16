package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.Event
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.common.withRemovedElementAt
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordBadgeNameUtil
import io.github.pelmenstar1.digiDict.data.RecordDao
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRecordBadgesViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val badgeDao: RecordBadgeDao
) : SingleDataLoadStateViewModel<Array<String>>(TAG) {
    val onRemoveError = Event()
    val onAddError = Event()

    override val canRefreshAfterSuccess: Boolean
        get() = true

    override fun DataLoadStateManager.FlowBuilder<Array<String>>.buildDataFlow() = fromFlow {
        badgeDao.getAllFlow()
    }

    fun add(name: String) {
        viewModelScope.launch {
            try {
                badgeDao.insert(RecordBadgeInfo(name))
            } catch (e: Exception) {
                Log.e(TAG, "", e)

                onAddError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
    }

    fun remove(name: String) {
        viewModelScope.launch {
            try {
                val encodedName = RecordBadgeNameUtil.encode(name)
                val records = recordDao.getRecordsByBadgeName(encodedName)

                for (record in records) {
                    val oldBadges = RecordBadgeNameUtil.decodeArray(record.rawBadges)
                    val badgeToDeleteIndex = oldBadges.indexOf(name)

                    val newBadges = if (badgeToDeleteIndex >= 0) {
                        oldBadges.withRemovedElementAt(badgeToDeleteIndex)
                    } else {
                        oldBadges
                    }

                    recordDao.updateBadges(record.id, RecordBadgeNameUtil.encodeArray(newBadges))
                }

                badgeDao.delete(name)
            } catch (e: Exception) {
                Log.e(TAG, "", e)

                onRemoveError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
    }

    companion object {
        private const val TAG = "ManageRecordBadges_VM"
    }
}