package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@HiltViewModel
class BadgeSelectorDialogViewModel @Inject constructor(
    badgeDao: RecordBadgeDao
) : ViewModel() {
    private val usedBadgesFlow = MutableStateFlow<Array<String>?>(null)
    private val allBadgesFlow = badgeDao.getAllFlow()

    var usedBadges: Array<String>?
        get() = usedBadgesFlow.value
        set(value) {
            usedBadgesFlow.value = value
        }

    val validBadgesFlow: Flow<Array<String>> = allBadgesFlow.combine(usedBadgesFlow.filterNotNull()) { all, used ->
        val result = ArrayList<String>(all.size)

        all.forEach {
            if (!used.contains(it)) {
                result.add(it)
            }
        }

        result.sort()
        result.toTypedArray()
    }
}