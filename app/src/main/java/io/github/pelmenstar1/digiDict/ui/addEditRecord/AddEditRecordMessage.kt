package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.resourcesMessageMapper

enum class AddEditRecordMessage {
    EMPTY_TEXT,
    EXISTING_EXPRESSION,
    EXPRESSION_NO_LETTER_OR_DIGIT;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<AddEditRecordMessage>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                EXISTING_EXPRESSION -> R.string.addEditRecord_existingExprError
                EXPRESSION_NO_LETTER_OR_DIGIT -> R.string.addEditRecord_exprNoLetterOrDigit
            }
        }
    }
}