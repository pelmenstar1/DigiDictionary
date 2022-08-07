package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.text.InputType
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.theme.overlay.MaterialThemeOverlay
import io.github.pelmenstar1.digiDict.EmptyArray
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.utils.*
import java.util.*

class MeaningListInteractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private class SavedState : AbsSavedState {
        var elements: Array<String>

        constructor(parcel: Parcel) : super(parcel) {
            val n = parcel.readInt()
            elements = Array(n) { parcel.readStringOrThrow() }
        }

        constructor(superState: Parcelable?) : super(superState) {
            elements = EmptyArray.STRING
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)

            dest.writeStringArray(elements)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }

    private var isError = true
    var onErrorStateChanged: ((state: Boolean) -> Unit)? = null

    var meaning: ComplexMeaning
        get() {
            // If there's only one element, meaning should be considered as "common"
            return elements.let {
                if (it.size == 1) {
                    ComplexMeaning.Common(it[0])
                } else {
                    ComplexMeaning.List(it)
                }
            }
        }
        set(value) {
            when (value) {
                is ComplexMeaning.Common -> {
                    adjustInputCount(1)

                    val inputLayout = getTextInputLayoutAt(0)

                    // Trim text just to make sure it's valid in terms of MeaningListInteractionView
                    // (elements should contain only trimmed strings)
                    //
                    // Side note: If a string is already "trimmed", trim() does not allocate,
                    // it returns the same instance.
                    val text = value.text.trim()

                    elements = arrayOf(text)

                    // Errors and other stuff are updated later.
                    withTextInputWatcherIgnored {
                        inputLayout.setText(text)
                    }
                }
                is ComplexMeaning.List -> {
                    val valueElements = value.elements
                    val newSize = valueElements.size

                    elements = Array(newSize) { "" }
                    adjustInputCount(newSize)

                    // Errors and other stuff are updated later.
                    withTextInputWatcherIgnored {
                        valueElements.forEachIndexed { index, element ->
                            // Trim element just to make sure it's valid in terms of MeaningListInteractionView
                            // (elements should contain only trimmed strings)
                            //
                            // Side note: If a string is already "trimmed", trim() does not allocate,
                            // it returns the same instance.
                            val trimmedElement = element.trim()

                            elements[index] = trimmedElement

                            getTextInputLayoutAt(index).setText(trimmedElement)
                        }
                    }
                }
            }

            refreshHintsAndEndButtons()
            refreshErrorState()
        }

    // There are some variables that are lazy-initialized.
    // It's done in such way because they are used rarely.
    //
    // If a variable is used in the initial state of the view, it's initialized in the constructor.

    private var ignoreTextInputWatcher = false

    private val emptyTextError: String
    private var duplicateError: String? = null
    private var noLetterOrDigitError: String? = null

    private var meaningAndOrdinalFormat: String? = null
    private val meaningStr: String

    private var endIconContentDescription: String? = null
    private var endIconDrawable: Drawable? = null

    private val textLayoutContext: Context

    // It should contain only trimmed strings (without leading and trailing whitespaces).
    private var elements = arrayOf("")

    private val cachedBitSet = BitSet()

    init {
        orientation = VERTICAL

        with(context.resources) {
            emptyTextError = getString(R.string.emptyTextError)
            meaningStr = getString(R.string.meaning)
        }

        textLayoutContext = MaterialThemeOverlay.wrap(
            context,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle,
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        )

        addView(createAddButton())

        // There should be at least one input.
        addNewItem(isUserInteraction = false)
    }

    private fun createAddButton() = MaterialButton(
        context,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        val res = resources
        val size = res.getDimensionPixelSize(R.dimen.addExpression_meaningAddButtonSize)

        layoutParams = LayoutParams(size, size).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        setPadding(0)
        insetTop = 0
        insetBottom = 0

        iconPadding = 0
        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        setIconResource(R.drawable.ic_add)

        setOnClickListener {
            elements = elements.withAddedElement("")

            addNewItem(isUserInteraction = true)
        }
    }

    private fun adjustInputCount(newCount: Int) {
        val currentCount = childCount - 1

        when {
            currentCount > newCount -> {
                removeViews(currentCount, newCount - currentCount)
            }
            currentCount < newCount -> {
                repeat(newCount - currentCount) {
                    addNewItem(isUserInteraction = false)
                }
            }
        }
    }

    private fun addNewItem(isUserInteraction: Boolean) {
        val index = childCount - 1

        val layout = TextInputLayout(
            textLayoutContext,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            layoutParams = listItemLayoutParams

            // When error icon is not null and error is not null, the error icon replaces end icon which is unwanted.
            errorIconDrawable = null

            // This is necessary because if the meaning is set through meaning setter
            // some text inputs will have different heights although they are initialized in the same way.
            // It's due to the fact that no error was set on some inputs. So to fix this, we should set isErrorEnabled to true.
            isErrorEnabled = true

            addView(
                TextInputEditText(textLayoutContext).apply {
                    layoutParams = listItemLayoutParams
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                    addTextChangedListener {
                        if (!ignoreTextInputWatcher) {
                            val currentText = it?.trimToString() ?: ""

                            elements[index] = currentText

                            refreshErrorState()
                        }
                    }
                })
        }

        addView(layout, index)
        refreshHintsAndEndButtons()

        // If an user intend to add a new input, then we should request a focus for the input
        if (isUserInteraction) {
            refreshErrorState()

            layout.requestFocus()
        }
    }

    private fun removeItem(index: Int) {
        elements = elements.withRemovedElementAt(index)

        removeViewAt(index)
        refreshHintsAndEndButtons()
        refreshErrorState()

        // Request focus for the nearest input.
        when {
            index > 0 -> getChildAt(index - 1).requestFocus()
            index < childCount - 1 -> getChildAt(index + 1).requestFocus()
        }
    }

    private fun setErrorState(value: Boolean) {
        if (isError != value) {
            isError = value
            onErrorStateChanged?.invoke(value)
        }
    }

    fun refreshErrorState() {
        var resultErrorState = false

        val duplicateBitSet = cachedBitSet.also {
            // Bitset needs to be empty.
            it.clear()
        }

        elements.iterateDuplicates { firstIndex, secondIndex ->
            duplicateBitSet.run {
                set(firstIndex)
                set(secondIndex)
            }
        }

        elements.forEachIndexed { index, element ->
            val inputLayout = getTextInputLayoutAt(index)

            val error = when {
                element.isBlank() -> ERROR_EMPTY_TEXT
                !element.containsLetterOrDigit() -> ERROR_NO_LETTER_OR_DIGIT
                duplicateBitSet[index] -> ERROR_DUPLICATE
                else -> ERROR_NONE
            }

            resultErrorState = resultErrorState or (error != ERROR_NONE)
            refreshErrorStateForTextLayout(inputLayout, error)
        }

        setErrorState(resultErrorState)
    }

    private fun refreshErrorStateForTextLayout(layout: TextInputLayout, state: Int) {
        val res = context.resources

        layout.error = when (state) {
            ERROR_EMPTY_TEXT -> emptyTextError
            ERROR_DUPLICATE -> res.getLazyString(
                duplicateError,
                R.string.addEditRecord_meaningDuplicateError,
            ) { duplicateError = it }
            ERROR_NO_LETTER_OR_DIGIT -> res.getLazyString(
                noLetterOrDigitError,
                R.string.addEditRecord_meaningNoLetterOrDigit,
            ) { noLetterOrDigitError = it }
            else -> null
        }
    }

    private fun refreshHintsAndEndButtons() {
        val childCount = childCount

        // If childCount is 2, then there's only one input.
        if (childCount == 2) {
            getTextInputLayoutAt(0).also {
                it.hint = meaningStr
                it.isEndIconVisible = false
            }
        } else {
            val context = context
            val theme = context.theme
            val res = context.resources
            val locale = context.getLocaleCompat()

            val format = res.getLazyString(
                meaningAndOrdinalFormat,
                R.string.meaningAndOrdinalFormat
            ) { meaningAndOrdinalFormat = it }

            iterateInputsIndexed { input, i ->
                // Index is 0-based, but user would except it to be 1-based, so +1
                input.hint = String.format(locale, format, i + 1)
                input.isEndIconVisible = true

                if (input.endIconDrawable == null) {
                    var endIcon = endIconDrawable
                    if (endIcon == null) {
                        endIcon = requireNotNull(
                            ResourcesCompat.getDrawable(
                                res,
                                R.drawable.ic_remove,
                                theme
                            )
                        )
                        endIconDrawable = endIcon
                    } else {
                        endIcon = requireNotNull(endIcon.constantState?.newDrawable())
                    }

                    input.endIconDrawable = endIcon

                    // When endIconDrawable is null, it means that endIconContentDescription and end icon on click listener
                    // are null and need to be initialized.
                    input.endIconContentDescription = getLazyValue(
                        endIconContentDescription,
                        { res.getString(R.string.remove) },
                        { endIconContentDescription = it }
                    )

                    input.setEndIconOnClickListener { removeItem(i) }
                }
            }
        }
    }

    private inline fun iterateInputsIndexed(block: (TextInputLayout, index: Int) -> Unit) {
        for (i in 0 until (childCount - 1)) {
            block(getTextInputLayoutAt(i), i)
        }
    }

    private inline fun iterateInputs(block: (TextInputLayout) -> Unit) =
        iterateInputsIndexed { it, _ -> block(it) }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getTextInputLayoutAt(index: Int) = getChildAt(index) as TextInputLayout

    private inline fun withTextInputWatcherIgnored(block: () -> Unit) {
        ignoreTextInputWatcher = true
        block()
        ignoreTextInputWatcher = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        iterateInputs {
            it.isEnabled = enabled
        }

        // "Add" button should be invisible when the view is disabled
        getChildAt(childCount - 1).visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            val stateElements = state.elements

            elements = stateElements
            adjustInputCount(stateElements.size)

            withTextInputWatcherIgnored {
                stateElements.forEachIndexed { index, element ->
                    getTextInputLayoutAt(index).setText(element)
                }
            }

            refreshHintsAndEndButtons()
            refreshErrorState()
        }

        super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.elements = elements
        }
    }

    companion object {
        private val listItemLayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        private const val ERROR_NONE = 0
        private const val ERROR_EMPTY_TEXT = 1
        private const val ERROR_DUPLICATE = 2
        private const val ERROR_NO_LETTER_OR_DIGIT = 3

        internal inline fun Resources.getLazyString(cached: String?, id: Int, set: (String) -> Unit): String {
            return getLazyValue(
                cached,
                { getString(id) },
                set
            )
        }

        /**
         * Finds all the duplicates in the given array and invokes [block] lambda for each of them.
         *
         * Time complexity: `O(n^2)` where n is size of the array.
         * It shouldn't be too much, because size of the meaning's items is expected to be small.
         */
        internal inline fun Array<String>.iterateDuplicates(block: (Int, Int) -> Unit) {
            for (i in indices) {
                val element = get(i)

                // Don't mark element as duplicate if it's blank, 'empty text' error should better be shown on that element.
                if (element.isNotBlank()) {
                    for (j in indices) {
                        val otherElement = get(j)

                        // No sense to check whether otherElement is blank, because non-blank element can't be equal to
                        // possible blank one.
                        if (i != j && element == otherElement) {
                            block(i, j)
                        }
                    }
                }
            }
        }
    }
}