package io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter

enum class AddWordToQueueDialogError {
    EMPTY_TEXT,
    WORD_EXISTS,
    WORD_NO_LETTER_OR_DIGIT,
    RECORD_EXPRESSION_EXISTS
}

class ResourcesAddWordToQueueDialogErrorStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<AddWordToQueueDialogError>(context, AddWordToQueueDialogError::class.java) {
    override fun getResourceId(value: AddWordToQueueDialogError): Int = when (value) {
        AddWordToQueueDialogError.EMPTY_TEXT -> R.string.emptyTextError
        AddWordToQueueDialogError.WORD_EXISTS -> R.string.addWordToQueue_wordExistsError
        AddWordToQueueDialogError.RECORD_EXPRESSION_EXISTS -> R.string.addWordToQueue_recordExprExistsError
        AddWordToQueueDialogError.WORD_NO_LETTER_OR_DIGIT -> R.string.addWordToQueue_noLetterOrDigitError
    }
}