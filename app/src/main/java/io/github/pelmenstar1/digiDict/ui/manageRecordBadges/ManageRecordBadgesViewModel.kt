package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.Event
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
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
    val onEditError = Event()

    override val canRefreshAfterSuccess: Boolean
        get() = true

    override fun DataLoadStateManager.FlowBuilder<Array<String>>.buildDataFlow() = fromFlow {
        badgeDao.getAllFlow()
    }

    fun edit(fromName: String, toName: String) {
        viewModelScope.launch {
            try {
                recordDao.changeRecordBadgeName(fromName, toName)
                badgeDao.updateName(fromName, toName)
            } catch (e: Exception) {
                Log.e(TAG, "", e)

                onEditError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
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
                recordDao.deleteBadgeFromAllRecords(name)
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