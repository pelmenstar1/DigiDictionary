package io.github.pelmenstar1.digiDict.common.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.fragment.app.DialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.TransparentDrawable

class LoadingIndicatorDialog : DialogFragment() {
    private var currentProgress = -1

    private var progressIndicator: CircularProgressIndicator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        if (currentProgress < 0) {
            currentProgress = savedInstanceState?.getInt(STATE_PROGRESS, 0) ?: 0
        }

        dialog?.window?.setBackgroundDrawable(TransparentDrawable)
        isCancelable = false

        return FrameLayout(context).apply {
            addView(CircularProgressIndicator(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER
                }

                progress = currentProgress

                progressIndicator = this
            })
        }
    }

    fun setProgress(@IntRange(from = 0, to = 100) value: Int) {
        currentProgress = value

        val indicator = progressIndicator
        if (indicator != null) {
            indicator.progress = value
        } else {
            currentProgress = value
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_PROGRESS, currentProgress)
    }

    companion object {
        private const val STATE_PROGRESS = "io.github.pelmenstar1.digiDict.common.ui.LoadingProgressDialog.progress"
    }
}