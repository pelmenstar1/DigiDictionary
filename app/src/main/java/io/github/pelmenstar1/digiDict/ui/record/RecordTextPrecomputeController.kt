package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.os.Build
import android.text.PrecomputedText
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

/**
 * Responsible for precomputing text values of a record.
 */
abstract class RecordTextPrecomputeController {
    private object Api21 : RecordTextPrecomputeController() {
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues? = null
    }

    @RequiresApi(28)
    private class Api28(private val context: Context) : RecordTextPrecomputeController() {
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues? {
            val params = params ?: return null

            val meaningFormattedText = MeaningTextHelper.formatOrErrorText(context, record.meaning)

            val exprPrecomputedText = PrecomputedText.create(record.expression, params.expressionParams)
            val meaningPrecomputedText = PrecomputedText.create(meaningFormattedText, params.meaningParams)

            return RecordTextPrecomputedValues(exprPrecomputedText, meaningPrecomputedText)
        }
    }

    /**
     * Gets or sets the [RecordTextPrecomputeParams] required for precomputing the text.
     *
     * If the value is null, [compute] returns `null`.
     */
    var params: RecordTextPrecomputeParams? = null

    /**
     * Precomputes text values of given [record].
     */
    abstract fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues?

    companion object {
        fun noOp(): RecordTextPrecomputeController {
            return Api21
        }

        /**
         * Creates new [RecordTextPrecomputeController] instance that will precompute text info using given
         * [params].
         *
         * If the API level >= 28, the implementation will actually precompute text values of record.
         * On lower API levels, the implementation will always return null in [compute] method.
         */
        fun create(context: Context): RecordTextPrecomputeController {
            return if (Build.VERSION.SDK_INT >= 28) Api28(context) else Api21
        }
    }
}