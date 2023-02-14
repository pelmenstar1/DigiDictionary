package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.android.*
import io.github.pelmenstar1.digiDict.common.ui.launchSetEnabledFlowCollector
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.databinding.FragmentQuizBinding
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.badge.BadgeContainer
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            setState(state)

            expressionView.text = value.expression
            badgeContainer.setBadges(value.badges)
            meaningView.apply {
                visibility = View.INVISIBLE
                text = MeaningTextHelper.parseToFormattedAndHandleErrors(context, value.meaning)
            }

            correctButton.initActionButton(isCorrect = true)
            wrongButton.initActionButton(isCorrect = false)

            container.setOnClickListener {
                setState(ITEM_STATE_EXTENDED)

                container.setOnClickListener(null)
            }
        }

        fun setState(state: Int) {
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

    private var itemStates = EmptyArray.BYTE

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

        // itemStates should be restored before it might be used.
        if (savedInstanceState != null) {
            itemStates = savedInstanceState.getByteArrayOrThrow(STATE_QUIZ_ITEM_STATES)
        }

        val binding = FragmentQuizBinding.inflate(inflater, container, false)
        val saveResultsButton = binding.quizSaveResults.also {
            it.setOnClickListener { vm.saveResults() }
        }

        val emptyTextView = binding.quizEmptyText
        val quizContainer = binding.quizContainer
        val itemsContainer = binding.quizItemsContainer

        vm.mode = args.mode
        popBackStackOnSuccess(vm.saveAction, navController)

        showSnackbarEventHandlerOnError(
            vm.saveAction,
            container,
            msgId = R.string.quiz_saveError,
            anchorView = saveResultsButton
        )

        ls.launchSetEnabledFlowCollector(saveResultsButton, vm.isAllAnswered)

        ls.launch {
            // Don't start a coroutine when it won't be used.
            //
            // Async here is used to load quiz data and 'break and hyphenation info' simultaneously.
            val breakAndHyphenationInfoAsync = if (Build.VERSION.SDK_INT >= 23) {
                async {
                    vm.textBreakAndHyphenationInfoSource.flow.first()
                }
            } else {
                null
            }

            quizContainer.setupLoadStateFlowSuspend(vm) {
                if (it.isEmpty()) {
                    emptyTextView.visibility = View.VISIBLE
                    saveResultsButton.visibility = View.GONE
                } else {
                    // Don't re-write and clear the content of itemStates if it's non-empty (restored from saved state)
                    if (itemStates.isEmpty()) {
                        itemStates = ByteArray(it.size)
                    }

                    submitItemsToContainer(itemsContainer, it)
                    emptyTextView.visibility = View.GONE
                    saveResultsButton.visibility = View.VISIBLE

                    if (Build.VERSION.SDK_INT >= 23) {
                        try {
                            val breakAndHyphenationInfo = breakAndHyphenationInfoAsync!!.await()

                            setBreakStrategyAndHyphenationToItems(itemsContainer, breakAndHyphenationInfo)
                        } catch (e: Exception) {
                            // PreferencesTextBreakAndHyphenationInfoSource possibly can throw exception
                            // when loading the data. On error here we do nothing, because it's not that
                            // critical that formatting is a little bit off.

                            Log.e(TAG, "failed to load break and hyphenation info", e)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun submitItemsToContainer(container: LinearLayout, items: Array<out ConciseRecordWithBadges>) {
        // submitItemsToContainer is expected to be called only once during life of the fragment.
        // This is just to be 100% sure that everything is fine.
        container.removeAllViews()

        for (i in items.indices) {
            val viewHolder = createItemViewHolder(container).also {
                it.bind(items[i], i, itemStates[i].toInt())
            }

            container.addView(viewHolder.container)
        }
    }

    @RequiresApi(23)
    private fun setBreakStrategyAndHyphenationToItems(container: LinearLayout, info: TextBreakAndHyphenationInfo) {
        val hf = info.hyphenationFrequency
        val bs = info.breakStrategy

        if (bs == BreakStrategy.UNSPECIFIED || hf == HyphenationFrequency.UNSPECIFIED) {
            throw IllegalStateException("break strategy and hyphenation info can't be unspecified")
        }

        val hfInt = hf.layoutInt
        val bsInt = bs.layoutInt

        for (i in 0 until container.childCount) {
            val itemContainer = container.getChildAt(i)

            val exprView = itemContainer.findViewById<TextView>(R.id.itemQuizRecord_expression)
            val meaningView = itemContainer.findViewById<TextView>(R.id.itemQuizRecord_meaning)

            exprView.breakStrategy = bsInt
            exprView.hyphenationFrequency = hfInt

            meaningView.breakStrategy = bsInt
            meaningView.hyphenationFrequency = hfInt
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

        outState.putByteArray(STATE_QUIZ_ITEM_STATES, itemStates)
    }

    companion object {
        private const val TAG = "QuizFragment"
        private const val STATE_QUIZ_ITEM_STATES = "io.github.pelmenstar1.digiDict.QuizFragment.quizState"

        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private const val ITEM_STATE_NOT_EXTENDED = 0
        private const val ITEM_STATE_EXTENDED = 1
        private const val ITEM_STATE_CORRECT = 2
        private const val ITEM_STATE_WRONG = 3
    }
}