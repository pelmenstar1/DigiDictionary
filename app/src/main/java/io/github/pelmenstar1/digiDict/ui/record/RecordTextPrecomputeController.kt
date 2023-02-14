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
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues? {
            return null
        }
    }

    @RequiresApi(28)
    private class Api28(private val context: Context) : RecordTextPrecomputeController() {
        override fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues {
            val meaningFormattedText = MeaningTextHelper.parseToFormattedAndHandleErrors(context, record.meaning)
            val params = params

            val exprPrecomputedText = PrecomputedText.create(record.expression, params.expressionParams)
            val meaningPrecomputedText = PrecomputedText.create(meaningFormattedText, params.meaningParams)

            return RecordTextPrecomputedValues(exprPrecomputedText, meaningPrecomputedText)
        }
    }

    private var _params: RecordTextPrecomputeParams? = null

    /**
     * Gets or sets the [RecordTextPrecomputeParams] required for precomputing the text.
     *
     * It will throw a [IllegalStateException] unless the [params] has a value, by default [params] has no value.
     */
    var params: RecordTextPrecomputeParams
        get() = _params ?: throw IllegalStateException("params is not initialized")
        set(value) {
            _params = value
        }

    /**
     * Precomputes text values of given [record].
     */
    abstract fun compute(record: ConciseRecordWithBadges): RecordTextPrecomputedValues?

    companion object {
        /**
         * Creates new [RecordTextPrecomputeController] instance that will precompute text info using given
         * [params].
         *
         * If the API level >= 28, the implementation will actually precompute text values of record.
         * On lower API levels, the implementation will always return null in [compute] method.
         */
        fun create(context: Context): RecordTextPrecomputeController {
            return if (Build.VERSION.SDK_INT >= 28) {
                Api28(context)
            } else {
                Api21
            }
        }
    }
}