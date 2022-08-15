package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.filterFast
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
    private val inputFlow = MutableStateFlow("")
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
        result.toArray(EmptyArray.STRING)
    }

    val badgesForSearchFlow: Flow<FilteredArray<String>> = validBadgesFlow.combine(inputFlow) { badges, input ->
        badges.filterFast { it.startsWith(input) }
    }

    val inputErrorFlow = usedBadgesFlow.filterNotNull().combine(inputFlow) { usedBadges, input ->
        when {
            input.isBlank() -> BadgeSelectorInputMessage.EMPTY_TEXT
            usedBadges.contains(input) -> BadgeSelectorInputMessage.EXISTS
            else -> null
        }
    }

    var input: String
        get() = inputFlow.value
        set(value) {
            inputFlow.value = value
        }
}