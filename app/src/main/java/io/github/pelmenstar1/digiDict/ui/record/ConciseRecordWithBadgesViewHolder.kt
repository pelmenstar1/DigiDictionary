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
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

open class ConciseRecordWithBadgesViewHolder private constructor(
    val container: ViewGroup
) : RecyclerView.ViewHolder(container) {
    private val expressionView: TextView
    val meaningView: TextView
    val scoreView: TextView

    private var meaningParseError: String? = null
    private var badgeContainer: BadgeContainer? = null

    constructor(context: Context) : this(createContainer(context))

    init {
        with(container.getTypedViewAt<ViewGroup>(MAIN_CONTENT_INDEX)) {
            expressionView = getTypedViewAt(EXPRESSION_VIEW_INDEX)
            meaningView = getTypedViewAt(MEANING_VIEW_INDEX)
            scoreView = getTypedViewAt(SCORE_VIEW_INDEX)
        }
    }

    private var positiveScoreColorList: ColorStateList? = null
    private var negativeScoreColorList: ColorStateList? = null

    private inline fun getLazyColorStateList(
        context: Context,
        @ColorRes res: Int,
        currentValue: ColorStateList?,
        set: (ColorStateList) -> Unit
    ): ColorStateList {
        return getLazyValue(
            currentValue,
            create = {
                ResourcesCompat.getColorStateList(context.resources, res, context.theme)!!
            },
            set
        )
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
                    getLazyColorStateList(
                        context, R.color.record_item_score_positive,
                        positiveScoreColorList
                    ) { positiveScoreColorList = it }
                } else {
                    getLazyColorStateList(
                        context, R.color.record_item_score_negative,
                        negativeScoreColorList
                    ) { negativeScoreColorList = it }
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
                    bc = createBadgeContainer(context)
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

        internal fun createContainer(context: Context): ViewGroup {
            return LinearLayout(context).apply {
                layoutParams = MATCH_WRAP_LAYOUT_PARAMS
                orientation = LinearLayout.VERTICAL
                setPaddingRes(R.dimen.itemRecord_padding)

                addView(createMainContentContainer(context))
            }
        }

        internal fun createBadgeContainer(context: Context): BadgeContainer {
            val res = context.resources

            return BadgeContainer(context).apply {
                layoutParams = MATCH_WRAP_LAYOUT_PARAMS

                res.getDimensionPixelOffset(R.dimen.itemRecord_badgeContainerHorizontalPadding).also {
                    setPadding(it, 0, it, 0)
                }
            }
        }

        private fun createMainContentContainer(context: Context): LinearLayout {
            val res = context.resources

            return LinearLayout(context).also { container ->
                container.layoutParams = MATCH_WRAP_LAYOUT_PARAMS

                container.addView(MaterialTextView(context).apply {
                    layoutParams = WRAP_WRAP_LAYOUT_PARAMS

                    setTextAppearance { BodyMedium }
                    setTextIsSelectable(false)
                })

                container.addView(MaterialTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 0.5f

                        marginStart = res.getDimensionPixelOffset(R.dimen.itemRecord_expressionMarginStart)
                    }

                    initTextView()
                })

                container.addView(MaterialTextView(context).apply {
                    layoutParams = MEANING_LAYOUT_PARAMS

                    textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                    initTextView()
                })
            }
        }


        private fun TextView.initTextView() {
            setTextAppearance { BodyLarge }
            maxLines = 100

            // Turn off ellipsizing
            ellipsize = null
        }
    }
}