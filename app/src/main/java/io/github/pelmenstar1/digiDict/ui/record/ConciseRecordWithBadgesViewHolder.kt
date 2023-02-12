package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.PrecomputedText
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.getNullableTypedViewAt
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

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

open class ConciseRecordWithBadgesViewHolder private constructor(
    val container: RecordItemRootContainer,
    protected val staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
) : RecyclerView.ViewHolder(container) {
    val expressionView: TextView
    val meaningView: TextView
    val scoreView: TextView

    constructor(context: Context, staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo) : this(
        createRootContainer(context, staticInfo), staticInfo
    )

    init {
        with(container.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)) {
            expressionView = getTypedViewAt(EXPRESSION_VIEW_INDEX)
            meaningView = getTypedViewAt(MEANING_VIEW_INDEX)
            scoreView = getTypedViewAt(SCORE_VIEW_INDEX)
        }
    }

    fun bind(
        record: ConciseRecordWithBadges?,
        hasDivider: Boolean,
        precomputedValues: RecordTextPrecomputedValues?,
        onContainerClickListener: View.OnClickListener
    ) {
        bind(container, record, hasDivider, precomputedValues, onContainerClickListener, staticInfo)
    }

    @RequiresApi(23)
    fun bindTextBreakAndHyphenationInfo(info: TextBreakAndHyphenationInfo) {
        Companion.bindTextBreakAndHyphenationInfo(container, info)
    }

    companion object {
        private const val TAG = "RecordViewHolder"

        private val MATCH_WRAP_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val WRAP_WRAP_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val MEANING_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.5f
        }

        // Indices relative to root container
        private const val MAIN_CONTENT_INDEX = 0
        private const val BADGE_CONTAINER_INDEX = 1

        // Indices relative to main-content container
        private const val SCORE_VIEW_INDEX = 0
        private const val EXPRESSION_VIEW_INDEX = 1
        private const val MEANING_VIEW_INDEX = 2

        fun setScore(scoreView: TextView, score: Int, staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo) {
            val textColorList = if (score >= 0) {
                staticInfo.positiveScoreColorList
            } else {
                staticInfo.negativeScoreColorList
            }

            scoreView.setTextColor(textColorList)
            scoreView.text = score.toString()
        }

        fun setBadges(
            root: RecordItemRootContainer,
            badges: Array<out RecordBadgeInfo>,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) {
            var bc = root.getNullableTypedViewAt<BadgeContainer>(BADGE_CONTAINER_INDEX)

            if (badges.isEmpty()) {
                bc?.removeAllViews()
            } else {
                if (bc == null) {
                    bc = createBadgeContainer(root.context, staticInfo)
                    root.addView(bc)
                }

                bc.setBadges(badges)
            }
        }

        fun bind(
            root: RecordItemRootContainer,
            record: ConciseRecordWithBadges?,
            hasDivider: Boolean,
            precomputedValues: RecordTextPrecomputedValues?,
            onContainerClickListener: View.OnClickListener,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) {
            // TODO: Add getters to the RecordItemRootContainer that extract views
            val mainContentContainer = root.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)
            val scoreView = mainContentContainer.getTypedViewAt<TextView>(SCORE_VIEW_INDEX)
            val expressionView = mainContentContainer.getTypedViewAt<TextView>(EXPRESSION_VIEW_INDEX)
            val meaningView = mainContentContainer.getTypedViewAt<TextView>(MEANING_VIEW_INDEX)

            root.hasDivider = hasDivider

            if (record != null) {
                val context = root.context

                root.tag = record
                root.setOnClickListener(onContainerClickListener)

                setExpressionAndMeaning(
                    context,
                    expressionView, meaningView,
                    record.expression, record.meaning,
                    precomputedValues
                )

                setScore(scoreView, record.score, staticInfo)
                setBadges(root, record.badges, staticInfo)
            } else {
                root.setOnClickListener(null)
                root.tag = null

                expressionView.text = ""
                meaningView.text = ""
                scoreView.text = ""
            }
        }

        @RequiresApi(23)
        fun bindTextBreakAndHyphenationInfo(root: RecordItemRootContainer, info: TextBreakAndHyphenationInfo) {
            val mainContentContainer = root.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)
            val expressionView = mainContentContainer.getTypedViewAt<TextView>(EXPRESSION_VIEW_INDEX)
            val meaningView = mainContentContainer.getTypedViewAt<TextView>(MEANING_VIEW_INDEX)

            expressionView.setTextBreakAndHyphenationInfo(info)
            meaningView.setTextBreakAndHyphenationInfo(info)
        }

        @RequiresApi(23)
        private fun TextView.setTextBreakAndHyphenationInfo(info: TextBreakAndHyphenationInfo) {
            val hf = info.hyphenationFrequency
            val bs = info.breakStrategy

            // TextView doesn't check whether the breakStrategy or hyphenationFrequency are really changed
            // and simply calls invalidate() and requestLayout().
            // As bindTextBreakAndHyphenationInfo can be called frequently,
            // change the value only when it matters.
            if (hyphenationFrequency != hf) {
                hyphenationFrequency = hf
            }

            if (breakStrategy != bs) {
                breakStrategy = bs
            }
        }

        private fun setExpressionAndMeaning(
            context: Context,
            exprView: TextView,
            meaningView: TextView,
            expr: String,
            meaning: String,
            precomputedValues: RecordTextPrecomputedValues?
        ) {
            var isExprSet = false
            var isMeaningSet = false

            if (Build.VERSION.SDK_INT >= 28 && precomputedValues != null) {
                isExprSet = exprView.trySetPrecomputedText(precomputedValues.expression)
                isMeaningSet = meaningView.trySetPrecomputedText(precomputedValues.meaning)
            }

            if (!isExprSet) {
                exprView.text = expr
            }

            if (!isMeaningSet) {
                meaningView.text = MeaningTextHelper.parseToFormattedAndHandleErrors(context, meaning)
            }
        }

        @RequiresApi(28)
        private fun TextView.trySetPrecomputedText(precomputed: PrecomputedText): Boolean {
            try {
                text = precomputed
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set precomputed text. Falling back to simple text", e)
            }

            return false
        }

        inline fun createOnItemClickListener(crossinline block: (id: Int) -> Unit) = View.OnClickListener {
            (it.tag as ConciseRecordWithBadges?)?.also { record -> block(record.id) }
        }

        fun createRootContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) = RecordItemRootContainer(context).apply {
            layoutParams = MATCH_WRAP_LAYOUT_PARAMS
            orientation = LinearLayout.VERTICAL

            dividerColor = staticInfo.dividerColor
            dividerHeight = staticInfo.dividerHeight

            setPadding(staticInfo.rootPadding)

            addView(createMainContentContainer(context, staticInfo))
        }

        private fun createBadgeContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) = BadgeContainer(context).apply {
            layoutParams = MATCH_WRAP_LAYOUT_PARAMS

            val hPadding = staticInfo.badgeContainerHorizontalPadding
            setPadding(hPadding, 0, hPadding, 0)
        }

        private fun createMainContentContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) = LinearLayout(context).also { container ->
            val bodyLargeTextAppearance = staticInfo.bodyLargeTextAppearance

            container.layoutParams = MATCH_WRAP_LAYOUT_PARAMS

            container.addView(MaterialTextView(context).apply {
                layoutParams = WRAP_WRAP_LAYOUT_PARAMS

                staticInfo.bodyMediumTextAppearance.apply(this)
                setTextIsSelectable(false)
            })

            container.addView(MaterialTextView(context).apply {
                layoutParams = staticInfo.expressionLayoutParams

                initTextView(bodyLargeTextAppearance)
            })

            container.addView(MaterialTextView(context).apply {
                layoutParams = MEANING_LAYOUT_PARAMS

                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                initTextView(bodyLargeTextAppearance)
            })
        }

        private fun TextView.initTextView(textAppearance: TextAppearance) {
            textAppearance.apply(this)
            maxLines = 100

            // Turn off ellipsizing
            ellipsize = null
        }
    }
}