package io.github.pelmenstar1.digiDict.ui.wordQueue

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance
import io.github.pelmenstar1.digiDict.data.WordQueueEntry

class WordQueueCard(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    private val wordView: TextView
    private val removeFromQueueButton: Button

    private var entry: WordQueueEntry? = null

    init {
        val res = context.resources

        radius = res.getDimension(R.dimen.wordQueueCard_radius)

        addView(LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }

            orientation = LinearLayout.HORIZONTAL

            addView(MaterialTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    weight = 1f

                    marginStart = res.getDimensionPixelOffset(R.dimen.wordQueueCard_wordViewStartMargin)
                }

                setTextAppearance { TitleMedium }

                wordView = this
            })

            addView(
                MaterialButton(
                    context,
                    null,
                    com.google.android.material.R.attr.materialIconButtonStyle
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }

                    setIconResource(R.drawable.ic_close)
                    setIconTintResource(R.color.color_on_primary)

                    removeFromQueueButton = this
                }
            )
        })
    }

    fun bind(value: WordQueueEntry) {
        entry = value

        wordView.text = value.word
    }

    fun setOnAddMeaningListener(listener: (WordQueueEntry) -> Unit) {
        setOnClickListener(createViewOnClickListener(listener))
    }

    fun setOnRemoveFromQueueListener(listener: (WordQueueEntry) -> Unit) {
        removeFromQueueButton.setOnClickListener(createViewOnClickListener(listener))
    }

    private fun createViewOnClickListener(listener: (WordQueueEntry) -> Unit) = OnClickListener {
        listener(entry!!)
    }
}