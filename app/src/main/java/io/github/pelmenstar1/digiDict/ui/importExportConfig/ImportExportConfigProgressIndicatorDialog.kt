package io.github.pelmenstar1.digiDict.ui.importExportConfig

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.AbstractProgressIndicatorDialog
import io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialogInterface
import io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialogManagerBase
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance

class ImportExportConfigProgressIndicatorDialog : AbstractProgressIndicatorDialog() {
    override fun createLayout(): Pair<ProgressBar, ViewGroup> {
        val context = requireContext()
        val res = context.resources

        val progressIndicator: CircularProgressIndicator
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            addView(CircularProgressIndicator(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }

                progressIndicator = this
            })

            addView(MaterialTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                    it.topMargin =
                        res.getDimensionPixelOffset(R.dimen.importExportConfigProgressIndicatorDialog_warningTopMargin)
                }

                setText(R.string.importExportConfig_doNotCloseAppWarning)
                setTextAppearance { BodyLarge }
                setTypeface(typeface, Typeface.BOLD)
            })
        }

        return progressIndicator to root
    }
}

class ImportExportConfigProgressIndicatorDialogManager : ProgressIndicatorDialogManagerBase() {
    override fun createDialog(): ProgressIndicatorDialogInterface {
        return ImportExportConfigProgressIndicatorDialog()
    }
}