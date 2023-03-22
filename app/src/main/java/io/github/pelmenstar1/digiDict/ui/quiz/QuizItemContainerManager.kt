package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

/**
 * Responsible for managing state of quiz item container.
 */
class QuizItemContainerManager(
    private val container: LinearLayout,
    private val onItemAnswer: (index: Int, isCorrect: Boolean) -> Unit
) {
    private inner class ItemViewHolder(@JvmField val container: ViewGroup) {
        @JvmField
        val expressionView: TextView
        @JvmField
        val meaningView: TextView
        private val correctButton: Button
        private val wrongButton: Button
        private val badgeContainer: BadgeContainer

        private var index = 0

        init {
            with(container) {
                expressionView = findViewById(R.id.itemQuizRecord_expression)
                meaningView = findViewById(R.id.itemQuizRecord_meaning)
                correctButton = findViewById(R.id.itemQuizRecord_correctAnswer)
                wrongButton = findViewById(R.id.itemQuizRecord_wrongAnswer)
                badgeContainer = findViewById(R.id.itemQuizRecord_badgeContainer)
            }
        }

        fun bind(value: ConciseRecordWithBadges, index: Int, state: Int) {
            this.index = index
            setState(state)

            expressionView.text = value.expression
            badgeContainer.setBadges(value.badges)
            meaningView.apply {
                visibility = View.INVISIBLE
                text = MeaningTextHelper.formatOrErrorText(context, value.meaning)
            }

            correctButton.initActionButton(isCorrect = true)
            wrongButton.initActionButton(isCorrect = false)

            container.setOnClickListener {
                setState(ITEM_STATE_EXTENDED)

                container.setOnClickListener(null)
            }
        }

        private fun Button.initActionButton(isCorrect: Boolean) {
            setOnClickListener {
                onItemAnswer(index, isCorrect)

                setState(if (isCorrect) ITEM_STATE_CORRECT else ITEM_STATE_WRONG)
            }
        }

        private fun setState(state: Int) {
            itemStates[index] = state.toByte()

            when (state) {
                ITEM_STATE_NOT_EXTENDED -> {
                    correctButton.visibility = View.GONE
                    wrongButton.visibility = View.GONE
                    meaningView.visibility = View.INVISIBLE
                }
                ITEM_STATE_EXTENDED -> {
                    correctButton.visibility = View.VISIBLE
                    wrongButton.visibility = View.VISIBLE
                    meaningView.visibility = View.VISIBLE
                }
                ITEM_STATE_CORRECT, ITEM_STATE_WRONG -> {
                    correctButton.visibility = View.GONE
                    wrongButton.visibility = View.GONE
                    meaningView.visibility = View.VISIBLE

                    container.background = if (state == ITEM_STATE_CORRECT) {
                        itemBackgroundHelper.getCorrectAnswerBackground()
                    } else {
                        itemBackgroundHelper.getWrongAnswerBackground()
                    }
                }
            }
        }
    }

    /**
     * Gets or sets an array of states for items.
     * Setting a new state array does not affect items already inflated in the container
     */
    var itemStates = EmptyArray.BYTE

    private var breakAndHyphenationInfo: TextBreakAndHyphenationInfo? = null

    private val layoutInflater: LayoutInflater
    private val itemBackgroundHelper: QuizItemBackgroundHelper

    init {
        val context = container.context

        layoutInflater = LayoutInflater.from(context)
        itemBackgroundHelper = QuizItemBackgroundHelper(context)
    }

    /**
     * Adds views, that represent specified [items], to the container.
     */
    fun submitItems(items: Array<out ConciseRecordWithBadges>) {
        // submitItemsToContainer is expected to be called only once during lifecycle of the fragment.
        // This is just to be 100% sure that everything is fine.
        container.removeAllViews()

        for (i in items.indices) {
            val viewHolder = createItemViewHolder(container).also {
                it.bind(items[i], i, itemStates[i].toInt())
            }

            container.addView(viewHolder.container)
        }
    }

    /**
     * Changes break strategy and hyphenation frequency of all items in the container.
     */
    @RequiresApi(23)
    fun setBreakStrategyAndHyphenationToItems(info: TextBreakAndHyphenationInfo) {
        val hf = info.hyphenationFrequency
        val bs = info.breakStrategy

        if (bs == BreakStrategy.UNSPECIFIED || hf == HyphenationFrequency.UNSPECIFIED) {
            throw IllegalStateException("break strategy and hyphenation info can't be unspecified")
        }

        breakAndHyphenationInfo = info

        for (i in 0 until container.childCount) {
            val itemContainer = container.getChildAt(i)

            val exprView = itemContainer.findViewById<TextView>(R.id.itemQuizRecord_expression)
            val meaningView = itemContainer.findViewById<TextView>(R.id.itemQuizRecord_meaning)

            setBreakAndHyphenationToItemContainer(exprView, meaningView, info)
        }
    }

    @RequiresApi(23)
    private fun setBreakAndHyphenationToItemContainer(
        exprView: TextView,
        meaningView: TextView,
        info: TextBreakAndHyphenationInfo
    ) {
        val bsInt = info.breakStrategy.layoutInt
        val hfInt = info.hyphenationFrequency.layoutInt

        exprView.breakStrategy = bsInt
        exprView.hyphenationFrequency = hfInt

        meaningView.breakStrategy = bsInt
        meaningView.hyphenationFrequency = hfInt
    }

    private fun createItemViewHolder(container: LinearLayout): ItemViewHolder {
        val view = layoutInflater.inflate(R.layout.item_quiz_record, container, false).apply {
            layoutParams = ITEM_LAYOUT_PARAMS
        }

        return ItemViewHolder(view as ViewGroup).also { vh ->
            if (Build.VERSION.SDK_INT >= 23) {
                breakAndHyphenationInfo?.also { info ->
                    setBreakAndHyphenationToItemContainer(vh.expressionView, vh.meaningView, info)
                }
            }
        }
    }

    companion object {
        private const val ITEM_STATE_NOT_EXTENDED = 0
        private const val ITEM_STATE_EXTENDED = 1
        private const val ITEM_STATE_CORRECT = 2
        private const val ITEM_STATE_WRONG = 3

        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

    }
}