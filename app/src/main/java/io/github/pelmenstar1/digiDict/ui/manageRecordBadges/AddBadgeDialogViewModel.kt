package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class AddBadgeDialogViewModel @Inject constructor(
    badgeDao: RecordBadgeDao
) : ViewModel() {
    private val allBadges = badgeDao.getAllFlow()
    private val inputFlow = MutableStateFlow("")

    val inputErrorFlow = allBadges.combine(inputFlow) { badges, input ->
        when {
            input.isBlank() -> AddBadgeInputMessage.EMPTY_TEXT
            badges.contains(input) -> AddBadgeInputMessage.EXISTS
            else -> null
        }
    }.onStart { emit(AddBadgeInputMessage.EMPTY_TEXT) }

    var input: String
        get() = inputFlow.value
        set(value) {
            inputFlow.value = value
        }
}