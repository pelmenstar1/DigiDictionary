package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.resourcesMessageMapper

enum class AddEditRecordMessage {
    EMPTY_TEXT,
    EXISTING_EXPRESSION;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<AddEditRecordMessage>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                EXISTING_EXPRESSION -> R.string.addEditRecord_existingExprError
            }
        }
    }
}