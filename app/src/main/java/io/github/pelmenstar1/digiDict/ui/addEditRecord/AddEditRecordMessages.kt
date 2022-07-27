package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.ResourcesMessageMapper

enum class AddEditRecordMessage {
    EMPTY_TEXT,
    EXISTING_EXPRESSION,
    DB_ERROR,
    TOO_MANY_MEANING_LIST_ITEMS,
    LOADING_ERROR
}

class AddEditExpressionMessageMapper(context: Context) :
    ResourcesMessageMapper<AddEditRecordMessage>(context, 5) {
    override fun mapToStringResource(type: AddEditRecordMessage) = when (type) {
        AddEditRecordMessage.EMPTY_TEXT -> R.string.emptyTextError
        AddEditRecordMessage.EXISTING_EXPRESSION -> R.string.addEditRecord_existingExprError
        AddEditRecordMessage.DB_ERROR -> R.string.dbError
        AddEditRecordMessage.TOO_MANY_MEANING_LIST_ITEMS -> R.string.addEditRecord_tooManyMeaningListItemsError
        AddEditRecordMessage.LOADING_ERROR -> R.string.recordLoadingError
    }
}