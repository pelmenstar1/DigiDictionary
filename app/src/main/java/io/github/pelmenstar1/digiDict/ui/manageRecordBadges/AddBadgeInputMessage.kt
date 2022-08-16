package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.resourcesMessageMapper

enum class AddBadgeInputMessage {
    EMPTY_TEXT,
    EXISTS;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<AddBadgeInputMessage>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                EXISTS -> R.string.addBadge_badgeExistsError
            }
        }
    }
}