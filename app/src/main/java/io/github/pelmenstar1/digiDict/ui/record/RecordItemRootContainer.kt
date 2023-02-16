package io.github.pelmenstar1.digiDict.ui.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.PrecomputedText
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.setPadding
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

/**
 * Represents a layout for record item to be used only in [ConciseRecordWithBadgesViewHolder]. That's why
 * it doesn't have a compatible constructor for XML layout.
 */
@SuppressLint("ViewConstructor")
class RecordItemRootContainer constructor(
    context: Context,
    private val staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
) : LinearLayout(context) {
    private val dividerPaint: Paint
    private val dividerHeight: Float

    private val mainContentContainer: LinearLayout
    private var badgeContainer: BadgeContainer? = null

    val expressionView: TextView
    val meaningView: TextView
    val scoreView: TextView

    /**
     * Gets or sets whether the item has a divider in bottom.
     */
    var hasDivider: Boolean = true
        set(value) {
            field = value

            invalidate()
        }

    init {
        setWillNotDraw(false)

        orientation = VERTICAL
        setPadding(staticInfo.rootPadding)

        dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = staticInfo.dividerColor
        }

        dividerHeight = staticInfo.dividerHeight

        addView(LinearLayout(context).apply {
            mainContentContainer = this
            layoutParams = MATCH_WRAP_LAYOUT_PARAMS

            val bodyLargeTextAppearance = staticInfo.bodyLargeTextAppearance

            addView(MaterialTextView(context).apply {
                scoreView = this
                layoutParams = WRAP_WRAP_LAYOUT_PARAMS

                staticInfo.bodyMediumTextAppearance.apply(this)
                setTextIsSelectable(false)
            })

            addView(MaterialTextView(context).apply {
                expressionView = this
                layoutParams = staticInfo.expressionLayoutParams

                initTextView(bodyLargeTextAppearance)
            })

            addView(MaterialTextView(context).apply {
                meaningView = this
                layoutParams = MEANING_LAYOUT_PARAMS

                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                initTextView(bodyLargeTextAppearance)
            })
        })
    }

    /**
     * Sets text for expression and meaning. Meaning is expected to be already formatted.
     *
     * Precomputed text values are optional and are only used on API level >= 28.
     */
    fun setExpressionAndMeaning(
        expr: CharSequence,
        meaning: CharSequence,
        precomputedValues: RecordTextPrecomputedValues? = null
    ) {
        var isExprSet = false
        var isMeaningSet = false

        if (Build.VERSION.SDK_INT >= 28 && precomputedValues != null) {
            isExprSet = expressionView.trySetPrecomputedText(precomputedValues.expression)
            isMeaningSet = meaningView.trySetPrecomputedText(precomputedValues.meaning)
        }

        if (!isExprSet) {
            expressionView.text = expr
        }

        if (!isMeaningSet) {
            meaningView.text = meaning
        }
    }

    /**
     * Sets badges for the item.
     */
    fun setBadges(badges: Array<out RecordBadgeInfo>) {
        var bc = badgeContainer

        if (badges.isEmpty()) {
            bc?.removeAllViews()
        } else {
            if (bc == null) {
                bc = createBadgeContainer()
                badgeContainer = bc

                addView(bc)
            }

            bc.setBadges(badges)
        }
    }

    /**
     * Sets a score value and properly formats its text view.
     */
    fun setScore(value: Int) {
        val textColorList = if (value >= 0) {
            staticInfo.positiveScoreColorList
        } else {
            staticInfo.negativeScoreColorList
        }

        scoreView.setTextColor(textColorList)
        scoreView.text = value.toString()
    }

    /**
     * Sets break strategy and hyphenation frequency for expression and meaning text views.
     */
    @RequiresApi(23)
    fun setTextBreakAndHyphenationInfo(value: TextBreakAndHyphenationInfo) {
        expressionView.setTextBreakAndHyphenationInfo(value)
        meaningView.setTextBreakAndHyphenationInfo(value)
    }

    @RequiresApi(23)
    private fun TextView.setTextBreakAndHyphenationInfo(info: TextBreakAndHyphenationInfo) {
        val hf = info.hyphenationFrequency.layoutInt
        val bs = info.breakStrategy.layoutInt

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

    private fun TextView.initTextView(textAppearance: TextAppearance) {
        textAppearance.apply(this)
        maxLines = 100

        // Turn off ellipsizing
        ellipsize = null
    }

    private fun createBadgeContainer() = BadgeContainer(context).apply {
        layoutParams = MATCH_WRAP_LAYOUT_PARAMS

        val hPadding = staticInfo.badgeContainerHorizontalPadding
        setPadding(hPadding, 0, hPadding, 0)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        if (hasDivider) {
            val w = width.toFloat()
            val h = height.toFloat()

            c.drawRect(0f, h - dividerHeight, w, h, dividerPaint)
        }
    }

    companion object {
        private const val TAG = "RecordItemRootContainer"

        private val WRAP_WRAP_LAYOUT_PARAMS = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        private val MATCH_WRAP_LAYOUT_PARAMS = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        private val MEANING_LAYOUT_PARAMS = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            weight = 0.5f
        }
    }
}