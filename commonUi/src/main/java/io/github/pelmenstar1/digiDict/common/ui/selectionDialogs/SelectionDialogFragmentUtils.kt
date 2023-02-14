package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import android.content.Context
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
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

    inline fun createAndAddViewsForItems(
        context: Context,
        choices: Array<out String>,
        root: LinearLayout,
        itemOnCheckedChangedListener: CompoundButton.OnCheckedChangeListener,
        createView: () -> CompoundButton,
        isChoiceChecked: (index: Int) -> Boolean
    ) {
        val res = context.resources

        val textVerticalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textVerticalPadding)
        val textHorizontalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textHorizontalPadding)
        val textAppearance = TextAppearance(context) { BodyLarge }

        val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        for ((index, choice) in choices.withIndex()) {
            root.addView(createView().apply {
                layoutParams = itemLayoutParams
                setPadding(textHorizontalPadding, textVerticalPadding, textHorizontalPadding, textVerticalPadding)

                tag = index
                text = choice
                isChecked = isChoiceChecked(index)
                textAppearance.apply(this)

                setOnCheckedChangeListener(itemOnCheckedChangedListener)
            })
        }
    }
}