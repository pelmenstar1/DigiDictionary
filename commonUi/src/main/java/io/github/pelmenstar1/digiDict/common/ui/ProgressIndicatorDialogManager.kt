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

class ProgressIndicatorDialogManager {
    private var progressCollectionJob: Job? = null
    private var coroutineScope: LifecycleCoroutineScope? = null
    private var progressFlow: Flow<Float>? = null
    private var fragmentManager: FragmentManager? = null

    fun init(fragment: Fragment, flow: Flow<Float>) {
        val fm = fragment.childFragmentManager

        coroutineScope = fragment.lifecycleScope
        fragmentManager = fm
        progressFlow = flow

        findLoadingIndicatorDialog(fm)?.also { dialog ->
            showOrBindLoadingIndicatorDialog(dialog)
        }
    }

    fun cancel() {
        progressCollectionJob?.cancel()
        hideLoadingProgressDialog()
    }

    fun showDialog() {
        showOrBindLoadingIndicatorDialog()
    }

    private fun showOrBindLoadingIndicatorDialog(currentDialog: ProgressIndicatorDialog? = null) {
        progressCollectionJob?.let {
            if (it.isActive) {
                debugLog(TAG) {
                    info("loadingProgressCollectionJob is already started")
                }

                it.cancel()
            }
        }

        val scope = coroutineScope ?: throwInitNotCalled()
        val pFlow = progressFlow ?: throwInitNotCalled()

        var dialog: ProgressIndicatorDialog? = currentDialog

        progressCollectionJob = scope.launch {
            pFlow.cancelAfter { it == 1f }.collect { progress ->
                debugLog(TAG) {
                    info("progress: $progress")
                }

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
                            tempDialog = findLoadingIndicatorDialog(fm)

                            if (tempDialog == null) {
                                // If there's no loading-indicator-dialog, show it.
                                tempDialog = ProgressIndicatorDialog().also {
                                    it.showNow(fm, LOADING_PROGRESS_DIALOG_TAG)
                                }
                            }

                            dialog = tempDialog
                        }

                        tempDialog.setProgress(progress)
                    }
                }
            }
        }.also {
            it.invokeOnCompletion { progressCollectionJob = null }
        }
    }

    private fun hideLoadingProgressDialog() {
        fragmentManager?.let { fm ->
            findLoadingIndicatorDialog(fm)?.dismissNow()
        }
    }

    private fun throwInitNotCalled(): Nothing = throw IllegalStateException("init() hasn't been called")

    companion object {
        private const val TAG = "ProgressIndicatorDialogManager"
        private const val LOADING_PROGRESS_DIALOG_TAG = "LoadingIndicatorDialog"

        internal fun findLoadingIndicatorDialog(fm: FragmentManager): ProgressIndicatorDialog? {
            return fm.findFragmentByTag(LOADING_PROGRESS_DIALOG_TAG) as ProgressIndicatorDialog?
        }
    }
}