package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.text.PrecomputedText
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

/**
 * Responsible for precomputing text values of a record.
 */
interface RecordTextPrecomputeController {
    private object Api21 : RecordTextPrecomputeController {
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues? {
            return null
        }
    }

    @RequiresApi(28)
    private class Api28(
        private val context: Context,
        private val params: RecordTextPrecomputeParams
    ) : RecordTextPrecomputeController {
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues {
            val meaningFormattedText = MeaningTextHelper.parseToFormattedAndHandleErrors(context, record.meaning)

            val exprPrecomputedText = PrecomputedText.create(record.expression, params.expressionParams)
            val meaningPrecomputedText = PrecomputedText.create(meaningFormattedText, params.meaningParams)

            return RecordTextPrecomputedValues(exprPrecomputedText, meaningPrecomputedText)
        }
    }

    /**
     * Precomputes text values of given [record].
     */
    fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues?

    companion object {
        /**
         * Returns [RecordTextPrecomputeController] instance that just returns `null` in [compute].
         *
         * It can be used as a fallback path when the API level < 28.
         */
        fun createNoOp(): RecordTextPrecomputeController {
            return Api21
        }

        /**
         * Creates new [RecordTextPrecomputeController] instance that will precompute text info using given
         * [params].
         *
         * As feature of precomputing text info is available since API level 28,
         * this method requires the API level >= 28 as well.
         */
        @RequiresApi(28)
        fun create(context: Context, params: RecordTextPrecomputeParams): RecordTextPrecomputeController {
            return Api28(context, params)
        }
    }
}