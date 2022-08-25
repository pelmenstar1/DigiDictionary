package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@HiltViewModel
class BadgeSelectorDialogViewModel @Inject constructor(
    badgeDao: RecordBadgeDao
) : ViewModel() {
    private val usedBadgeIdsFlow = MutableStateFlow<IntArray?>(null)
    private val allBadgesFlow = badgeDao.getAllFlow()

    var usedBadgeIds: IntArray?
        get() = usedBadgeIdsFlow.value
        set(value) {
            usedBadgeIdsFlow.value = value
        }

    val validBadgesFlow = allBadgesFlow.combine(usedBadgeIdsFlow.filterNotNull()) { all, used ->
        val result = ArrayList<RecordBadgeInfo>(all.size)

        all.forEach { badge ->
            if (!used.contains(badge.id)) {
                result.add(badge)
            }
        }

        result
    }
}