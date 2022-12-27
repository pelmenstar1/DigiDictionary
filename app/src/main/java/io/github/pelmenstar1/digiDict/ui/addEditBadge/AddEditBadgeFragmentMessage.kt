package io.github.pelmenstar1.digiDict.ui.addEditBadge

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.resourcesMessageMapper

enum class AddEditBadgeFragmentMessage {
    EMPTY_TEXT,
    NAME_EXISTS;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<AddEditBadgeFragmentMessage>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                NAME_EXISTS -> R.string.addEditBadge_nameExistsError
            }
        }
    }
}