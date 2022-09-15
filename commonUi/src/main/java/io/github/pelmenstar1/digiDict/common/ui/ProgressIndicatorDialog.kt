package io.github.pelmenstar1.digiDict.common.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.fragment.app.DialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.TransparentDrawable

class ProgressIndicatorDialog : DialogFragment() {
    private var currentProgress = Float.NaN
    private var progressIndicator: CircularProgressIndicator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        if (currentProgress.isNaN()) {
            currentProgress = savedInstanceState?.getFloat(STATE_PROGRESS, 0f) ?: 0f
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

                progress = (currentProgress * 100f + 0.5f).toInt()

                progressIndicator = this
            })
        }
    }

    fun setProgress(@FloatRange(from = 0.0, to = 1.0) value: Float) {
        currentProgress = value
        progressIndicator?.progress = (value * 100f + 0.5f).toInt()

        /*
        debugLog("ProgressIndicatorDialog") {
            info("progress: ${(value * 100f + 0.5f).toInt()}")
        }

         */
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putFloat(STATE_PROGRESS, currentProgress)
    }

    companion object {
        private const val STATE_PROGRESS = "io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialog.progress"
    }
}