package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter

enum class AddEditRecordMessage {
    EMPTY_TEXT,
    EXISTING_EXPRESSION,
    EXPRESSION_NO_LETTER_OR_DIGIT
}

class ResourcesAddEditRecordMessageStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<AddEditRecordMessage>(context, AddEditRecordMessage::class.java) {
    override fun getResourceId(value: AddEditRecordMessage): Int = when (value) {
        AddEditRecordMessage.EMPTY_TEXT -> R.string.emptyTextError
        AddEditRecordMessage.EXISTING_EXPRESSION -> R.string.addEditRecord_existingExprError
        AddEditRecordMessage.EXPRESSION_NO_LETTER_OR_DIGIT -> R.string.addEditRecord_exprNoLetterOrDigit
    }
}