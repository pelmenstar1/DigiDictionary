package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.resourcesMessageMapper

enum class BadgeSelectorInputMessage {
    EMPTY_TEXT,
    EXISTS;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<BadgeSelectorInputMessage>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                EXISTS -> R.string.badgeSelector_badgeExistsError
            }
        }
    }
}