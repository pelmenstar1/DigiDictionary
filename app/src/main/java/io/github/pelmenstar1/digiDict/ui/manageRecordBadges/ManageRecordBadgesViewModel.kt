package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.Event
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelationDao
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRecordBadgesViewModel @Inject constructor(
    private val recordToBadgeRelationDao: RecordToBadgeRelationDao,
    private val badgeDao: RecordBadgeDao
) : SingleDataLoadStateViewModel<Array<RecordBadgeInfo>>(TAG) {
    val onRemoveError = Event()
    val onAddError = Event()
    val onEditError = Event()

    override val canRefreshAfterSuccess: Boolean
        get() = true

    override fun DataLoadStateManager.FlowBuilder<Array<RecordBadgeInfo>>.buildDataFlow() = fromFlow {
        badgeDao.getAllFlow()
    }

    fun update(value: RecordBadgeInfo) {
        // TODO: Add helper to generalize viewModelScope.launch { try { ... } catch(e: Exception) { ... } }
        viewModelScope.launch {
            try {
                badgeDao.update(value)
            } catch (e: Exception) {
                Log.e(TAG, "", e)

                onEditError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
    }

    fun add(value: RecordBadgeInfo) {
        viewModelScope.launch {
            try {
                badgeDao.insert(value)
            } catch (e: Exception) {
                Log.e(TAG, "", e)

                onAddError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
    }

    fun remove(badge: RecordBadgeInfo) {
        viewModelScope.launch {
            try {
                recordToBadgeRelationDao.deleteAllBadgeRelations(badge.id)
                badgeDao.delete(badge)
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