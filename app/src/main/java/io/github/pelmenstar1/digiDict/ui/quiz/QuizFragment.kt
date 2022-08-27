package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.ui.launchSetEnabledFlowCollector
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.databinding.FragmentQuizBinding
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer

@AndroidEntryPoint
class QuizFragment : Fragment() {
    private inner class ItemViewHolder(val container: ViewGroup) {
        private val expressionView: TextView
        private val meaningView: TextView
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

            expressionView.text = value.expression
            badgeContainer.setBadges(value.badges)
            meaningView.apply {
                visibility = View.INVISIBLE
                text = MeaningTextHelper.parseToFormattedAndHandleErrors(context, value.meaning)
            }

            correctButton.initActionButton(isCorrect = true)
            wrongButton.initActionButton(isCorrect = false)

            setState(state)

            container.setOnClickListener {
                setState(ITEM_STATE_EXTENDED)

                container.setOnClickListener(null)
            }
        }

        fun setState(state: Int) {
            itemsState = itemsState.withItemState(index, state)

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

        private fun Button.initActionButton(isCorrect: Boolean) {
            setOnClickListener {
                onItemAnswer(index, isCorrect)

                setState(if (isCorrect) ITEM_STATE_CORRECT else ITEM_STATE_WRONG)
            }
        }
    }

    private val args by navArgs<QuizFragmentArgs>()
    private val viewModel by viewModels<QuizViewModel>()

    private lateinit var itemBackgroundHelper: QuizItemBackgroundHelper

    private var itemsState = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val vm = viewModel
        val navController = findNavController()
        val ls = lifecycleScope
        itemBackgroundHelper = QuizItemBackgroundHelper(context)

        val binding = FragmentQuizBinding.inflate(inflater, container, false)

        vm.mode = args.mode
        popBackStackOnSuccess(vm.saveAction, navController)

        with(binding) {
            showSnackbarEventHandlerOnError(
                vm.saveAction,
                container,
                msgId = R.string.quiz_saveError,
                anchorView = quizSaveResults
            )

            quizSaveResults.run {
                ls.launchSetEnabledFlowCollector(this, vm.isAllAnswered)

                setOnClickListener { vm.saveResults() }
            }

            quizContainer.setupLoadStateFlow(ls, vm) {
                if (it.isEmpty()) {
                    quizEmptyText.visibility = View.VISIBLE
                    quizSaveResults.visibility = View.GONE
                } else {
                    submitItemsToContainer(quizItemsContainer, it)

                    quizEmptyText.visibility = View.GONE
                    quizSaveResults.visibility = View.VISIBLE
                }
            }
        }

        if (savedInstanceState != null) {
            itemsState = savedInstanceState.getInt(STATE_QUIZ_STATE)
        }

        return binding.root
    }

    private fun submitItemsToContainer(container: LinearLayout, items: Array<out ConciseRecordWithBadges>) {
        container.removeAllViews()

        for (i in items.indices) {
            val viewHolder = createItemViewHolder(container).also {
                it.bind(items[i], i, itemsState.getItemState(i))
            }

            container.addView(viewHolder.container)
        }
    }

    private fun createItemViewHolder(container: LinearLayout): ItemViewHolder {
        val view = layoutInflater.inflate(R.layout.item_quiz_record, container, false).apply {
            layoutParams = ITEM_LAYOUT_PARAMS
        }

        return ItemViewHolder(view as ViewGroup)
    }

    internal fun onItemAnswer(index: Int, isCorrect: Boolean) {
        viewModel.onItemAnswer(index, isCorrect)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_QUIZ_STATE, itemsState)
    }

    companion object {
        private const val STATE_QUIZ_STATE = "io.github.pelmenstar1.digiDict.QuizFragment.quizState"

        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private const val ITEM_STATE_NOT_EXTENDED = 0
        private const val ITEM_STATE_EXTENDED = 1
        private const val ITEM_STATE_CORRECT = 2
        private const val ITEM_STATE_WRONG = 3

        private const val STATE_BITS_COUNT = 3
        private const val STATE_MASK = 0x7

        internal fun Int.getItemState(index: Int): Int {
            return (this shr (index * STATE_BITS_COUNT)) and STATE_MASK
        }

        internal fun Int.withItemState(index: Int, state: Int): Int {
            val shift = index * STATE_BITS_COUNT

            return (this and (STATE_MASK shl shift).inv()) or (state shl shift)
        }
    }
}