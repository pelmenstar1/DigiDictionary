package io.github.pelmenstar1.digiDict.ui.paging

import android.content.Context
import android.text.PrecomputedText
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

interface RecordPageItemPrecomputeController {
    private object Api21 : RecordPageItemPrecomputeController {
        override fun compute(record: ConciseRecordWithBadges): PageItem.Record.PrecomputedInfo? {
            return null
        }
    }

    @RequiresApi(29)
    private class Api29(
        private val context: Context,
        private val params: RecordPageItemPrecomputeParams
    ) : RecordPageItemPrecomputeController {
        override fun compute(record: ConciseRecordWithBadges): PageItem.Record.PrecomputedInfo {
            val meaningFormattedText = MeaningTextHelper.parseToFormattedAndHandleErrors(context, record.meaning)

            val exprPrecomputedText = PrecomputedText.create(record.expression, params.expressionParams)
            val meaningPrecomputedText = PrecomputedText.create(meaningFormattedText, params.meaningParams)

            return PageItem.Record.PrecomputedInfo(exprPrecomputedText, meaningPrecomputedText)
        }
    }

    fun compute(record: ConciseRecordWithBadges): PageItem.Record.PrecomputedInfo?

    companion object {
        fun createNoOp(): RecordPageItemPrecomputeController {
            return Api21
        }

        @RequiresApi(29)
        fun create(context: Context, params: RecordPageItemPrecomputeParams): RecordPageItemPrecomputeController {
            return Api29(context, params)
        }
    }
}