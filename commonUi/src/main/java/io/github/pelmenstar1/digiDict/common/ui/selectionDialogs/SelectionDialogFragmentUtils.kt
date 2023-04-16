package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.ui.R

internal object SelectionDialogFragmentUtils {
    fun createTitleView(context: Context, @StringRes titleRes: Int): TextView {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = res.getDimensionPixelOffset(R.dimen.selectionDialog_titleMarginStart)
            }

            text = res.getText(titleRes)
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_DigiDictionary_SelectionDialog_Title)
        }
    }
}