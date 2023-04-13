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

/**
 * Represents a base for classes that manage dialogs with progress indicators.
 * These dialogs should implement [ProgressIndicatorDialogInterface].
 *
 * The progress is collected from [Flow], of [Int]s. Several assumptions about the flow are made:
 * - The valid progress should be between 0 and 100.
 * - If progress [ProgressReporter.UNREPORTED] is emitted, the logic does nothing.
 * - If progress [ProgressReporter.ERROR] is emitted, it must be the last item emitted and the dialog is immediately
 * dismissed.
 * - 100 progress should be the last progress emitted.
 * Basically, this flow should be retrieved from [ProgressReporter.progressFlow].
 */
abstract class ProgressIndicatorDialogManagerBase {
    private var progressCollectionJob: Job? = null
    private var coroutineScope: LifecycleCoroutineScope? = null
    private var progressFlow: Flow<Int>? = null
    private var fragmentManager: FragmentManager? = null

    /**
     * Initializes the manager. If the dialog with [PROGRESS_INDICATOR_DIALOG_TAG] is already attached to [fragment]'s child fragment manager,
     * the method will start using that dialog.
     *
     * @param fragment a host fragment where the dialog, created by [createDialog], should be shown
     * @param progressFlow a flow from where the progress can be collected.
     */
    fun init(fragment: Fragment, progressFlow: Flow<Int>) {
        val fm = fragment.childFragmentManager

        coroutineScope = fragment.lifecycleScope
        fragmentManager = fm
        this.progressFlow = progressFlow

        (fm.findFragmentByTag(PROGRESS_INDICATOR_DIALOG_TAG) as ProgressIndicatorDialogInterface?)?.also { dialog ->
            showOrBindProgressIndicatorDialog(dialog)
        }
    }

    /**
     * Shows the dialog.
     *
     * The main assumption is that the dialog with [PROGRESS_INDICATOR_DIALOG_TAG] should not be attached to
     * the host fragment's child fragment manager. This case is handled in [init].
     */
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
        val fm = fragmentManager ?: throwInitNotCalled()

        var dialog = currentDialog

        progressCollectionJob = scope.launch {
            pFlow.cancelAfter { it == 100 || it == ProgressReporter.ERROR }.collect { progress ->
                if (progress == 100 || progress == ProgressReporter.ERROR) {
                    dialog?.dismissNow()
                } else if (progress != ProgressReporter.UNREPORTED) { // we should not show dialog if progress is UNREPORTED
                    var tempDialog = dialog
                    if (tempDialog == null) {
                        tempDialog = createDialog().also {
                            it.showNow(fm, PROGRESS_INDICATOR_DIALOG_TAG)
                        }

                        dialog = tempDialog
                    }

                    tempDialog.setProgress(progress)
                }
            }
        }.also {
            it.invokeOnCompletion { progressCollectionJob = null }
        }
    }

    private fun throwInitNotCalled(): Nothing = throw IllegalStateException("init() hasn't been called")

    /**
     * Creates the dialog to be shown.
     */
    protected abstract fun createDialog(): ProgressIndicatorDialogInterface

    companion object {
        private const val TAG = "ProgressIndicatorDialogManager"
        const val PROGRESS_INDICATOR_DIALOG_TAG = "LoadingIndicatorDialog"
    }
}

class SimpleProgressIndicatorDialogManager : ProgressIndicatorDialogManagerBase() {
    override fun createDialog(): ProgressIndicatorDialogInterface {
        return SimpleProgressIndicatorDialog()
    }
}