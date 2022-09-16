package io.github.pelmenstar1.digiDict.common.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.cancelAfter
import io.github.pelmenstar1.digiDict.common.debugLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class ProgressIndicatorDialogManagerBase {
    private var progressCollectionJob: Job? = null
    private var coroutineScope: LifecycleCoroutineScope? = null
    private var progressFlow: Flow<Float>? = null
    private var fragmentManager: FragmentManager? = null

    fun init(fragment: Fragment, flow: Flow<Float>) {
        val fm = fragment.childFragmentManager

        coroutineScope = fragment.lifecycleScope
        fragmentManager = fm
        progressFlow = flow

        findProgressIndicatorDialog(fm)?.also { dialog ->
            showOrBindProgressIndicatorDialog(dialog)
        }
    }

    fun cancel() {
        progressCollectionJob?.cancel()
        hideProgressIndicatorDialog()
    }

    fun showDialog() {
        showOrBindProgressIndicatorDialog()
    }

    private fun showOrBindProgressIndicatorDialog(currentDialog: ProgressIndicatorDialogInterface? = null) {
        progressCollectionJob?.let {
            if (it.isActive) {
                debugLog(TAG) {
                    info("progressCollectionJob is already started")
                }

                it.cancel()
            }
        }

        val scope = coroutineScope ?: throwInitNotCalled()
        val pFlow = progressFlow ?: throwInitNotCalled()

        var dialog: ProgressIndicatorDialogInterface? = currentDialog

        progressCollectionJob = scope.launch {
            pFlow.cancelAfter { it == 1f }.collect { progress ->
                when (progress) {
                    1f -> {
                        dialog?.dismissNow()

                        // To be sure dialog won't be reused after it's dismissed.
                        dialog = null
                    }
                    ProgressReporter.ERROR -> {
                        dialog?.dismissNow()
                    }
                    ProgressReporter.UNREPORTED -> {}
                    else -> {
                        var tempDialog = dialog
                        if (tempDialog == null) {
                            val fm = fragmentManager ?: throwInitNotCalled()

                            // Try to find existing dialog to re-use it.
                            tempDialog = findProgressIndicatorDialog(fm)

                            if (tempDialog == null) {
                                // If there's no loading-indicator-dialog, show it.
                                tempDialog = createDialog().also {
                                    it.showNow(fm, PROGRESS_INDICATOR_DIALOG_TAG)
                                }
                            }

                            dialog = tempDialog
                        }

                        tempDialog.setProgress((progress * 100f + 0.5f).toInt())
                    }
                }
            }
        }.also {
            it.invokeOnCompletion { progressCollectionJob = null }
        }
    }

    private fun hideProgressIndicatorDialog() {
        fragmentManager?.let { fm ->
            findProgressIndicatorDialog(fm)?.dismissNow()
        }
    }

    private fun throwInitNotCalled(): Nothing = throw IllegalStateException("init() hasn't been called")

    protected abstract fun createDialog(): ProgressIndicatorDialogInterface

    companion object {
        private const val TAG = "ProgressIndicatorDialogManager"
        private const val PROGRESS_INDICATOR_DIALOG_TAG = "LoadingIndicatorDialog"

        internal fun findProgressIndicatorDialog(fm: FragmentManager): ProgressIndicatorDialogInterface? {
            return fm.findFragmentByTag(PROGRESS_INDICATOR_DIALOG_TAG) as ProgressIndicatorDialogInterface?
        }
    }
}

class SimpleProgressIndicatorDialogManager : ProgressIndicatorDialogManagerBase() {
    override fun createDialog(): ProgressIndicatorDialogInterface {
        return SimpleProgressIndicatorDialog()
    }
}