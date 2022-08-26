package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class AddEditBadgeDialogViewModel @Inject constructor(
    badgeDao: RecordBadgeDao
) : ViewModel() {
    private val allBadges = badgeDao.getAllFlow()
    private val nameFlow = MutableStateFlow("")

    var currentBadgeName: String? = null
        set(value) {
            field = value
            value?.let { name = it }
        }

    val nameErrorFlow = allBadges.combine(nameFlow) { badges, name ->
        val curBadgeName = currentBadgeName

        when {
            name.isEmpty() ->
                AddEditBadgeInputMessage.EMPTY_TEXT
            (curBadgeName == null || curBadgeName != name) && badges.any { it.name == name } ->
                AddEditBadgeInputMessage.EXISTS
            else -> null
        }
    }

    /**
     * Name of the badge to be created. The string is expected to be without trailing and leading whitespaces.
     */
    var name: String
        get() = nameFlow.value
        set(value) {
            nameFlow.value = value
        }
}