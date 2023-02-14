package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

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
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.R

/**
 * Represents a base class for dialog fragments that provides a list of choices and only a single item can be selected.
 */
abstract class SingleSelectionDialogFragment<TValue> : MaterialDialogFragment() {
    /**
     * Gets or sets a lambda that is called when value is selected.
     */
    var onValueSelected: ((TValue) -> Unit)? = null

    /**
     * Gets an array resource that stores a string array of possible choices.
     *
     * If [choicesInfoRes] is not 0, the size of this array should be the same as an array of [choicesInfoRes].
     */
    @get:ArrayRes
    protected abstract val choicesRes: Int

    /**
     * Gets a string resource that stores a title of the dialog
     */
    @get:StringRes
    protected abstract val titleRes: Int

    /**
     * Gets an array resource that stores a string array that contains an additional information for each corresponding choice.
     *
     * It can return 0 as a mark that such info should not be shown and doesn't exist.
     * If resource id is not 0, the size of the array should be the same as an array of [choicesRes].
     */
    @get:ArrayRes
    protected open val choicesInfoRes: Int
        get() = 0

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
        val itemOnCheckedChangedListener = createItemOnCheckedChangeListener()

        val res = context.resources

        val textVerticalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textVerticalPadding)
        val textHorizontalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textHorizontalPadding)

        val buttonTextAppearance = TextAppearance(context) { BodyLarge }

        // Variables related to info will be initialized if choicesInfoRes is not 0 to avoid unnecessary overhead.
        var infoTextAppearance: TextAppearance? = null
        var infoLayoutParams: LinearLayout.LayoutParams? = null

        val choices = res.getStringArray(choicesRes)
        val selectedIndex = extractSelectedIndex(choices.size)

        var choicesInfoArray: Array<String>? = null
        val choicesInfoRes = choicesInfoRes

        if (choicesInfoRes != 0) {
            choicesInfoArray = res.getStringArray(choicesInfoRes)

            infoTextAppearance = TextAppearance(context) { BodyMedium }
            infoLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = res.getDimensionPixelOffset(R.dimen.selectionDialog_infoStartMargin)
                bottomMargin = res.getDimensionPixelOffset(R.dimen.selectionDialog_infoBottomMargin)
            }

            if (choicesInfoArray.size != choices.size) {
                throw IllegalStateException("Size of array of choicesInfoRes is expected to be the same as size of array of choicesRes")
            }
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        for ((index, choice) in choices.withIndex()) {
            root.addView(MaterialRadioButton(context).apply {
                layoutParams = lp
                setPadding(textHorizontalPadding, textVerticalPadding, textHorizontalPadding, textVerticalPadding)

                tag = index
                text = choice
                isChecked = index == selectedIndex
                buttonTextAppearance.apply(this)

                setOnCheckedChangeListener(itemOnCheckedChangedListener)
            })

            if (choicesInfoArray != null) {
                root.addView(MaterialTextView(context).apply {
                    layoutParams = infoLayoutParams
                    text = choicesInfoArray[index]

                    infoTextAppearance?.apply(this)
                })
            }
        }
    }

    private fun createItemOnCheckedChangeListener(): CompoundButton.OnCheckedChangeListener {
        return CompoundButton.OnCheckedChangeListener { button, _ ->
            val index = button.tag as Int

            onValueSelected?.invoke(getValueByIndex(index))
            dismiss()
        }
    }

    private fun extractSelectedIndex(choicesLength: Int): Int {
        val args = arguments ?: throw IllegalStateException("Fragment's arguments bundle is expected to be non-null")
        val index = args.getInt(ARGS_SELECTED_INDEX, -1)

        if (index !in 0 until choicesLength) {
            throw IllegalStateException("Selected index is out of bounds (index=$index, length=$choicesLength)")
        }

        return index
    }

    /**
     * Returns [TValue] instance that corresponds to specified [index] in array of [choicesRes].
     */
    protected abstract fun getValueByIndex(index: Int): TValue

    companion object {
        private const val ARGS_SELECTED_INDEX =
            "io.github.pelmenstar1.digiDict.SingleSelectionDialogFragment.selectedIndex"

        /**
         * Creates a [Bundle] of arguments to be used in the dialog fragment.
         *
         * @param selectedIndex an index of value that should be selected when the fragment is created.
         * Should be non-negative and less than length of [choicesRes] array. Although the upper bound is not checked
         * in this method, an [IllegalStateException] will be thrown when fragment is created.
         */
        fun createArguments(selectedIndex: Int): Bundle {
            require(selectedIndex >= 0) { "selectedIndex can't be negative" }

            return Bundle(1).apply {
                putInt(ARGS_SELECTED_INDEX, selectedIndex)
            }
        }
    }
}