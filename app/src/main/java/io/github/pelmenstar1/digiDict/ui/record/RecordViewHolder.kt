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
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.utils.getLazyValue

class RecordViewHolder private constructor(
    private val container: ViewGroup
) : RecyclerView.ViewHolder(container) {
    private val expressionView = container.getTextViewAt(EXPRESSION_VIEW_INDEX)
    private val meaningView = container.getTextViewAt(MEANING_VIEW_INDEX)
    private val scoreView = container.getTextViewAt(SCORE_VIEW_INDEX)

    constructor(context: Context) : this(createContainer(context))

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

    fun bind(record: Record?, onContainerClickListener: View.OnClickListener) {
        if (record != null) {
            container.tag = record

            expressionView.text = record.expression
            meaningView.text = MeaningTextHelper.parseToFormatted(record.rawMeaning)

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

            container.setOnClickListener(onContainerClickListener)
        } else {
            container.setOnClickListener(null)

            expressionView.text = ""
            meaningView.text = ""
            scoreView.text = ""
        }
    }

    companion object {
        private val CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val SCORE_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val MEANING_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.5f
        }

        private const val SCORE_VIEW_INDEX = 0
        private const val EXPRESSION_VIEW_INDEX = 1
        private const val MEANING_VIEW_INDEX = 2

        inline fun createOnItemClickListener(crossinline block: (id: Int) -> Unit) = View.OnClickListener {
            (it.tag as? Record?)?.also { (id) -> block(id) }
        }

        internal fun createContainer(context: Context): ViewGroup {
            return LinearLayout(context).also { container ->
                container.layoutParams = CONTAINER_LAYOUT_PARAMS

                val density = context.resources.displayMetrics.density
                val dp5 = (5f * density + 0.5f).toInt()

                container.setPadding(dp5)

                container.addView(MaterialTextView(context).apply {
                    layoutParams = SCORE_LAYOUT_PARAMS

                    TextViewCompat.setTextAppearance(
                        this,
                        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
                    )
                    setTextIsSelectable(false)
                    isClickable = false
                })

                container.addView(MaterialTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 0.5f

                        leftMargin = dp5
                        rightMargin = dp5
                    }

                    TextViewCompat.setTextAppearance(
                        this,
                        com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
                    )
                    isClickable = false

                    initMultilineTextView()
                })

                container.addView(MaterialTextView(context).apply {
                    layoutParams = MEANING_LAYOUT_PARAMS

                    TextViewCompat.setTextAppearance(
                        this,
                        com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
                    )
                    textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END

                    initMultilineTextView()
                    isClickable = false
                })
            }
        }

        internal fun ViewGroup.getTextViewAt(index: Int) = getChildAt(index) as TextView

        private fun TextView.initMultilineTextView() {
            maxLines = 100

            // Turn off ellipsizing
            ellipsize = null
        }
    }
}