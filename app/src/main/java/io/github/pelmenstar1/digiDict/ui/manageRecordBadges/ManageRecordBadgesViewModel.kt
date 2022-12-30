package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelationDao
import javax.inject.Inject

@HiltViewModel
class ManageRecordBadgesViewModel @Inject constructor(
    private val recordToBadgeRelationDao: RecordToBadgeRelationDao,
    private val badgeDao: RecordBadgeDao
) : SingleDataLoadStateViewModel<Array<RecordBadgeInfo>>(TAG) {
    val removeAction = viewModelAction<RecordBadgeInfo>(TAG) { badge ->
        recordToBadgeRelationDao.deleteAllBadgeRelations(badge.id)
        badgeDao.delete(badge)
    }

    override val canRefreshAfterSuccess: Boolean
        get() = true

    override fun DataLoadStateManager.FlowBuilder<Array<RecordBadgeInfo>>.buildDataFlow() = fromFlow {
        badgeDao.getAllFlow()
    }

    fun remove(badge: RecordBadgeInfo) = removeAction.run(badge)

    companion object {
        private const val TAG = "ManageRecordBadges_VM"
    }
}