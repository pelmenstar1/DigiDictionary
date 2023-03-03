package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.TextPaint
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

class ConciseRecordWithBadgesViewHolderStaticInfo(context: Context) {
    private val res = context.resources
    private val theme = context.theme

    private var _positiveScoreColorList: ColorStateList? = null
    private var _negativeScoreColorList: ColorStateList? = null
    private var _badgeContainerHorizontalPadding = -1

    private var _expressionMeaningTextPaintForMeasure: TextPaint? = null

    val positiveScoreColorList: ColorStateList
        get() = getLazyColorStateList(
            R.color.record_item_score_positive,
            _positiveScoreColorList
        ) { _positiveScoreColorList = it }

    val negativeScoreColorList: ColorStateList
        get() = getLazyColorStateList(
            R.color.record_item_score_negative,
            _negativeScoreColorList
        ) { _negativeScoreColorList = it }

    val badgeContainerHorizontalPadding: Int
        get() {
            var padding = _badgeContainerHorizontalPadding
            if (padding < 0) {
                padding =
                    res.getDimensionPixelOffset(R.dimen.itemRecord_badgeContainerHorizontalPadding)
                _badgeContainerHorizontalPadding = padding
            }

            return padding
        }

    val bodyMediumTextAppearance = TextAppearance(context) { BodyMedium }
    val bodyLargeTextAppearance = TextAppearance(context) { BodyLarge }

    val expressionLayoutParams = LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        weight = 0.5f
        marginStart = res.getDimensionPixelOffset(R.dimen.itemRecord_expressionMarginStart)
    }

    val dividerHeight = res.getDimension(R.dimen.itemRecord_dividerHeight)
    val dividerColor = ResourcesCompat.getColor(res, R.color.record_item_divider, theme)

    val rootPadding = res.getDimensionPixelOffset(R.dimen.itemRecord_padding)

    /**
     * Gets the [TextPaint] instance that copies those properties of [TextPaint] in expression [TextView]
     * that affect the text measuring.
     *
     * The returned value is cached and shouldn't be mutated.
     */
    val expressionTextPaintForMeasure: TextPaint
        get() = getExpressionMeaningTextPaintForMeasure()

    /**
     * Gets the [TextPaint] instance that copies those properties of [TextPaint] in meaning [TextView]
     * that affect the text measuring.
     *
     * The returned value is cached and shouldn't be mutated.
     */
    val meaningTextPaintForMeasure: TextPaint
        get() = getExpressionMeaningTextPaintForMeasure()

    private fun getExpressionMeaningTextPaintForMeasure(): TextPaint {
        return getLazyValue(
            _expressionMeaningTextPaintForMeasure,
            { bodyLargeTextAppearance.getTextPaintForMeasure() },
            { _expressionMeaningTextPaintForMeasure = it }
        )
    }

    private inline fun getLazyColorStateList(
        @ColorRes colorRes: Int,
        currentValue: ColorStateList?,
        set: (ColorStateList) -> Unit
    ): ColorStateList {
        return getLazyValue(
            currentValue,
            create = {
                ResourcesCompat.getColorStateList(res, colorRes, theme)!!
            },
            set
        )
    }
}

open class ConciseRecordWithBadgesViewHolder(
    val container: RecordItemRootContainer,
    onItemClickListener: View.OnClickListener
) : RecyclerView.ViewHolder(container) {
    constructor(
        context: Context,
        onItemClickListener: View.OnClickListener,
        staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
    ) : this(createRootContainer(context, staticInfo), onItemClickListener)

    init {
        container.setOnClickListener(onItemClickListener)
    }

    fun bind(
        record: ConciseRecordWithBadges,
        hasDivider: Boolean,
        precomputedValues: RecordTextPrecomputedValues?
    ) {
        val formattedMeaning = MeaningTextHelper.formatOrErrorText(container.context, record.meaning)

        container.hasDivider = hasDivider

        container.tag = record

        container.setExpressionAndMeaning(record.expression, formattedMeaning, precomputedValues)
        container.setScore(record.score)
        container.setBadges(record.badges)
    }

    @RequiresApi(23)
    fun setTextBreakAndHyphenationInfo(info: TextBreakAndHyphenationInfo) {
        container.setTextBreakAndHyphenationInfo(info)
    }

    fun setTextBreakAndHyphenationInfoCompat(info: TextBreakAndHyphenationInfo?) {
        if (Build.VERSION.SDK_INT >= 23) {
            info?.also { container.setTextBreakAndHyphenationInfo(it) }
        }
    }

    companion object {
        private val MATCH_WRAP_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        inline fun createOnItemClickListener(crossinline block: (id: Int) -> Unit) = View.OnClickListener {
            (it.tag as ConciseRecordWithBadges?)?.also { record -> block(record.id) }
        }

        fun createRootContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) = RecordItemRootContainer(context, staticInfo).apply {
            layoutParams = MATCH_WRAP_LAYOUT_PARAMS
        }
    }
}