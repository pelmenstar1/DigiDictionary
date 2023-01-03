package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance

abstract class SingleSelectionDialogFragment<TValue> : MaterialDialogFragment() {
    var onValueSelected: ((TValue) -> Unit)? = null

    @get:ArrayRes
    protected abstract val choicesRes: Int

    @get:StringRes
    protected abstract val titleRes: Int

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val res = context.resources

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            val verticalPadding = res.getDimensionPixelOffset(R.dimen.singleSelectionDialog_rootVerticalPadding)
            val horizontalPadding = res.getDimensionPixelOffset(R.dimen.singleSelectionDialog_rootHorizontalPadding)

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        root.addView(createTitleView(context))
        createAndAddViewsForItems(context, root)

        return ScrollView(context).apply {
            addView(root)
        }
    }

    private fun createTitleView(context: Context): TextView {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = res.getDimensionPixelOffset(R.dimen.singleSelectionDialog_titleMarginStart)
            }

            text = res.getText(titleRes)
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_DigiDictionary_ConstListDialog_Title)
        }
    }

    private fun createAndAddViewsForItems(context: Context, root: LinearLayout) {
        val res = context.resources

        val choices = res.getStringArray(choicesRes)
        val textVerticalPadding = res.getDimensionPixelOffset(R.dimen.singleSelectionDialog_textVerticalPadding)
        val textHorizontalPadding = res.getDimensionPixelOffset(R.dimen.singleSelectionDialog_textHorizontalPadding)
        val textAppearance = TextAppearance(context) { BodyLarge }

        val values = getValues()

        val itemOnClickListener = View.OnClickListener {
            @Suppress("UNCHECKED_CAST")
            val value = it.tag as TValue

            onValueSelected?.invoke(value)
            dismiss()
        }

        val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val selectedIndex = arguments?.getInt(ARGS_SELECTED_INDEX) ?: -1

        for ((index, choice) in choices.withIndex()) {
            root.addView(MaterialRadioButton(context).apply {
                layoutParams = itemLayoutParams
                setPadding(textHorizontalPadding, textVerticalPadding, textHorizontalPadding, textVerticalPadding)

                tag = values[index]
                text = choice
                textAppearance.apply(this)

                setOnClickListener(itemOnClickListener)

                isChecked = index == selectedIndex
            })
        }
    }

    // TODO: Use direct approach with getting the TValue by index
    protected abstract fun getValues(): Array<out TValue>

    companion object {
        private const val ARGS_SELECTED_INDEX =
            "io.github.pelmenstar1.digiDict.SingleSelectionDialogFragment.selectedIndex"

        fun createArguments(selectedIndex: Int): Bundle {
            return Bundle(1).apply {
                putInt(ARGS_SELECTED_INDEX, selectedIndex)
            }
        }
    }
}