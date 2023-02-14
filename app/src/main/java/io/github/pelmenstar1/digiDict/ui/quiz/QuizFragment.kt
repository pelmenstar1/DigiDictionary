package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.getByteArrayOrThrow
import io.github.pelmenstar1.digiDict.common.android.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.launchSetEnabledFlowCollector
import io.github.pelmenstar1.digiDict.databinding.FragmentQuizBinding

@AndroidEntryPoint
class QuizFragment : Fragment() {
    private val args by navArgs<QuizFragmentArgs>()
    private val viewModel by viewModels<QuizViewModel>()

    private lateinit var itemContainerManager: QuizItemContainerManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val ls = lifecycleScope
        val navController = findNavController()

        val binding = FragmentQuizBinding.inflate(inflater, container, false)
        val saveResultsButton = binding.quizSaveResults.also {
            it.setOnClickListener { vm.saveResults() }
        }

        val emptyTextView = binding.quizEmptyText
        val quizContainer = binding.quizContainer
        val itemsContainer = binding.quizItemsContainer

        itemContainerManager = QuizItemContainerManager(itemsContainer, vm::onItemAnswer)

        // itemStates should be restored before it's used.
        var itemStates = savedInstanceState?.getByteArrayOrThrow(STATE_QUIZ_ITEM_STATES)

        vm.mode = args.mode
        popBackStackOnSuccess(vm.saveAction, navController)

        showSnackbarEventHandlerOnError(
            vm.saveAction,
            container,
            msgId = R.string.quiz_saveError,
            anchorView = saveResultsButton
        )

        ls.launchSetEnabledFlowCollector(saveResultsButton, vm.isAllAnswered)

        if (Build.VERSION.SDK_INT >= 23) {
            try {
                ls.launchFlowCollector(vm.textBreakAndHyphenationInfoSource.flow) { info ->
                    itemContainerManager.setBreakStrategyAndHyphenationToItems(info)
                }
            } catch (e: Exception) {
                // PreferencesTextBreakAndHyphenationInfoSource possibly can throw exception
                // on collecting the data. On error here we do nothing, because it's not that
                // critical that formatting is a little bit off.

                Log.e(TAG, "failed to load break and hyphenation info", e)
            }
        }

        quizContainer.setupLoadStateFlow(ls, vm) { items ->
            if (items.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
                saveResultsButton.visibility = View.GONE
            } else {
                // Don't re-write and clear the content of itemStates if it's non-empty (restored from saved state)
                if (itemStates == null) {
                    itemStates = ByteArray(items.size)
                }

                itemContainerManager.also { manager ->
                    manager.itemStates = itemStates!!
                    manager.submitItems(items)
                }

                emptyTextView.visibility = View.GONE
                saveResultsButton.visibility = View.VISIBLE
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putByteArray(STATE_QUIZ_ITEM_STATES, itemContainerManager.itemStates)
    }

    companion object {
        private const val TAG = "QuizFragment"

        private const val STATE_QUIZ_ITEM_STATES = "io.github.pelmenstar1.digiDict.QuizFragment.quizState"
    }
}