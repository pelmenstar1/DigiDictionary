package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment

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

            val verticalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_rootVerticalPadding)
            val horizontalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_rootHorizontalPadding)

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        root.addView(SelectionDialogFragmentUtils.createTitleView(context, titleRes))
        createAndAddViewsForItems(context, root)

        return ScrollView(context).apply { addView(root) }
    }

    private fun createAndAddViewsForItems(context: Context, root: LinearLayout) {
        val itemOnCheckedChangedListener = CompoundButton.OnCheckedChangeListener { button, _ ->
            val index = button.tag as Int

            onValueSelected?.invoke(getValueByIndex(index))
            dismiss()
        }

        val choices = resources.getStringArray(choicesRes)
        val selectedIndex = arguments?.getInt(ARGS_SELECTED_INDEX) ?: -1

        SelectionDialogFragmentUtils.createAndAddViewsForItems(
            context,
            choices,
            root,
            itemOnCheckedChangedListener,
            createView = { MaterialRadioButton(context) },
            isChoiceChecked = { selectedIndex == it }
        )
    }

    protected abstract fun getValueByIndex(index: Int): TValue

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