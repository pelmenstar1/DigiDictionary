package io.github.pelmenstar1.digiDict.common.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.IntRange
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.android.TransparentDrawable

interface ProgressIndicatorDialogInterface {
    fun setProgress(value: Int)

    fun showNow(fm: FragmentManager, tag: String?)
    fun dismissNow()
}

abstract class AbstractProgressIndicatorDialog : DialogFragment(), ProgressIndicatorDialogInterface {
    private var currentProgress = -1

    @JvmField
    internal var progressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (currentProgress < 0) {
            currentProgress = savedInstanceState?.getInt(STATE_PROGRESS, 0) ?: 0
        }

        dialog?.window?.setBackgroundDrawable(TransparentDrawable)
        isCancelable = false

        return createLayout().let { (pb, root) ->
            pb.progress = currentProgress
            progressBar = pb

            root
        }
    }

    override fun setProgress(@IntRange(from = 0, to = 100) value: Int) {
        currentProgress = value
        progressBar?.progress = value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_PROGRESS, currentProgress)
    }

    protected abstract fun createLayout(): Pair<ProgressBar, ViewGroup>

    companion object {
        private const val STATE_PROGRESS =
            "io.github.pelmenstar1.digiDict.common.ui.AbstractProgressIndicatorDialog.progress"
    }
}

class SimpleProgressIndicatorDialog : AbstractProgressIndicatorDialog() {
    override fun createLayout(): Pair<ProgressBar, ViewGroup> {
        val progressIndicator: CircularProgressIndicator
        val root = FrameLayout(requireContext()).apply {
            addView(CircularProgressIndicator(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER
                }

                progressIndicator = this
            })
        }

        return progressIndicator to root
    }
}