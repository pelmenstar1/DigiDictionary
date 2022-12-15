package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.getNullableTypedViewAt
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
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
    private val staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
) : RecyclerView.ViewHolder(container) {
    val meaningView: TextView
    val scoreView: TextView

    constructor(context: Context, staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo) : this(
        createRootContainer(context, staticInfo), staticInfo
    )

    init {
        with(container.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)) {
            meaningView = getTypedViewAt(MEANING_VIEW_INDEX)
            scoreView = getTypedViewAt(SCORE_VIEW_INDEX)
        }
    }

    fun bind(record: ConciseRecordWithBadges?, hasDivider: Boolean, onContainerClickListener: View.OnClickListener) {
        bind(container, record, hasDivider, onContainerClickListener, staticInfo)
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

        // Indices relative to root container
        private const val MAIN_CONTENT_INDEX = 0
        private const val BADGE_CONTAINER_INDEX = 1

        // Indices relative to main-content container
        private const val SCORE_VIEW_INDEX = 0
        private const val EXPRESSION_VIEW_INDEX = 1
        private const val MEANING_VIEW_INDEX = 2

        fun bind(
            root: RecordItemRootContainer,
            record: ConciseRecordWithBadges?,
            hasDivider: Boolean,
            onContainerClickListener: View.OnClickListener,
            staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
        ) {
            val mainContentContainer = root.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)
            val scoreView = mainContentContainer.getTypedViewAt<TextView>(SCORE_VIEW_INDEX)
            val expressionView = mainContentContainer.getTypedViewAt<TextView>(EXPRESSION_VIEW_INDEX)
            val meaningView = mainContentContainer.getTypedViewAt<TextView>(MEANING_VIEW_INDEX)

            root.hasDivider = hasDivider

            if (record != null) {
                val context = root.context

                root.tag = record
                root.setOnClickListener(onContainerClickListener)

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
                var bc = root.getNullableTypedViewAt<BadgeContainer>(BADGE_CONTAINER_INDEX)

                if (badges.isEmpty()) {
                    bc?.removeAllViews()
                } else {
                    if (bc == null) {
                        bc = createBadgeContainer(context, staticInfo)
                        root.addView(bc)
                    }

                    bc.setBadges(record.badges)
                }
            } else {
                root.setOnClickListener(null)
                root.tag = null

                expressionView.text = ""
                meaningView.text = ""
                scoreView.text = ""
            }
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

        internal fun createBadgeContainer(
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