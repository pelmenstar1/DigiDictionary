package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

class ConciseRecordWithBadgesViewHolderStaticInfo(context: Context) {
    private val res = context.resources
    private val theme = context.theme

    private var _positiveScoreColorList: ColorStateList? = null
    private var _negativeScoreColorList: ColorStateList? = null
    private var _badgeContainerHorizontalPadding = -1

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
                padding = res.getDimensionPixelOffset(R.dimen.itemRecord_badgeContainerHorizontalPadding)
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
    val container: ViewGroup,
    private val staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
) : RecyclerView.ViewHolder(container) {
    private val expressionView: TextView
    val meaningView: TextView
    val scoreView: TextView

    private var badgeContainer: BadgeContainer? = null

    constructor(context: Context, staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo) : this(
        createContainer(context, staticInfo), staticInfo
    )

    init {
        with(container.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)) {
            expressionView = getTypedViewAt(EXPRESSION_VIEW_INDEX)
            meaningView = getTypedViewAt(MEANING_VIEW_INDEX)
            scoreView = getTypedViewAt(SCORE_VIEW_INDEX)
        }
    }

    fun bind(record: ConciseRecordWithBadges?, onContainerClickListener: View.OnClickListener) {
        if (record != null) {
            val context = container.context

            container.tag = record
            container.setOnClickListener(onContainerClickListener)

            expressionView.text = record.expression
            meaningView.text = MeaningTextHelper.parseToFormattedAndHandleErrors(context, record.meaning)

            scoreView.run {
                val score = record.score

                val textColorList = if (score >= 0) {
                    staticInfo.positiveScoreColorList
                } else {
                    staticInfo.negativeScoreColorList
                }

                setTextColor(textColorList)
                text = score.toString()
            }

            val badges = record.badges
            var bc = badgeContainer

            if (badges.isEmpty()) {
                bc?.removeAllViews()
            } else {
                if (bc == null) {
                    bc = createBadgeContainer(context, staticInfo)
                    container.addView(bc)
                }

                bc.setBadges(record.badges)
            }
        } else {
            container.setOnClickListener(null)
            container.tag = null

            expressionView.text = ""
            meaningView.text = ""
            scoreView.text = ""
        }
    }

    companion object {
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

        private const val MAIN_CONTENT_INDEX = 0

        private const val SCORE_VIEW_INDEX = 0
        private const val EXPRESSION_VIEW_INDEX = 1
        private const val MEANING_VIEW_INDEX = 2

        inline fun createOnItemClickListener(crossinline block: (id: Int) -> Unit) = View.OnClickListener {
            (it.tag as ConciseRecordWithBadges?)?.also { record -> block(record.id) }
        }

        internal fun createContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ): ViewGroup {
            return LinearLayout(context).apply {
                layoutParams = MATCH_WRAP_LAYOUT_PARAMS
                orientation = LinearLayout.VERTICAL
                setPaddingRes(R.dimen.itemRecord_padding)

                addView(createMainContentContainer(context, staticInfo))
            }
        }

        internal fun createBadgeContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ): BadgeContainer {
            return BadgeContainer(context).apply {
                layoutParams = MATCH_WRAP_LAYOUT_PARAMS

                val hPadding = staticInfo.badgeContainerHorizontalPadding
                setPadding(hPadding, 0, hPadding, 0)
            }
        }

        private fun createMainContentContainer(
            context: Context,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ): LinearLayout {
            val bodyLargeTextAppearance = staticInfo.bodyLargeTextAppearance

            return LinearLayout(context).also { container ->
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
        }

        private fun TextView.initTextView(textAppearance: TextAppearance) {
            textAppearance.apply(this)
            maxLines = 100

            // Turn off ellipsizing
            ellipsize = null
        }
    }
}