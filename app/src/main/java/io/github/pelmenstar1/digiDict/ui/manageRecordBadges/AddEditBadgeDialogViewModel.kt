package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class AddEditBadgeDialogViewModel @Inject constructor(
    badgeDao: RecordBadgeDao
) : ViewModel() {
    private val allBadges = badgeDao.getAllFlow()
    private val inputFlow = MutableStateFlow("")

    var currentBadgeName: String? = null
        set(value) {
            field = value
            value?.let { input = it }
        }

    val inputErrorFlow = allBadges.combine(inputFlow) { badges, input ->
        val curBadgeName = currentBadgeName

        when {
            input.isBlank() ->
                AddEditBadgeInputMessage.EMPTY_TEXT
            (curBadgeName == null || curBadgeName != input) && badges.indexOfFirst { it.name == input } >= 0 ->
                AddEditBadgeInputMessage.EXISTS
            else -> null
        }
    }.onStart { emit(AddEditBadgeInputMessage.EMPTY_TEXT) }

    var input: String
        get() = inputFlow.value
        set(value) {
            inputFlow.value = value
        }
}