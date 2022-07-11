package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.AttributeSet
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
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.utils.*

class MeaningListInteractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var isError = true
    var onErrorStateChanged: ((state: Boolean) -> Unit)? = null

    var onTooManyItems: (() -> Unit)? = null

    var meaning: ComplexMeaning
        get() {
            // If childCount is 2,
            // then there's only one input (except add button) and meaning should be considered as "common"
            return if (childCount == 2) {
                val layout = getTextInputLayoutAt(0)

                ComplexMeaning.Common(layout.getText().trimToString())
            } else {
                ComplexMeaning.List(getItems())
            }
        }
        set(value) {
            when (value) {
                is ComplexMeaning.Common -> {
                    adjustInputCount(1)

                    val layout = getTextInputLayoutAt(0)
                    val text = value.text

                    // Update error state if necessary
                    val errorState = text.isBlank()
                    setErrorState(errorState)
                    refreshErrorStateForTextLayout(layout, errorState)

                    layout.setText(text)
                }
                is ComplexMeaning.List -> {
                    val elements = value.elements
                    val newCount = elements.size

                    adjustInputCount(newCount)

                    iterateInputsIndexed { input, i ->
                        val element = elements[i]
                        val isError = element.isBlank()

                        if (isError) {
                            setErrorState(true)
                        }

                        refreshErrorStateForTextLayout(input, isError)
                        input.setText(element)
                    }
                }
            }
        }

    private val emptyTextError: String
    private var meaningAndOrdinalFormat: String? = null
    private val meaningStr: String
    private val removeStr: String
    private var endIconDrawable: Drawable? = null

    init {
        orientation = VERTICAL

        with(context.resources) {
            emptyTextError = getString(R.string.emptyTextError)
            meaningStr = getString(R.string.meaning)
            removeStr = getString(R.string.remove)
        }

        addView(createAddButton())

        // There should be at least one input.
        addNewItem(isUserInteraction = false)
    }

    private fun createAddButton() = MaterialButton(
        context,
        null,
        ADD_BUTTON_STYLE_ATTR
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

        if (index >= 100) {
            onTooManyItems?.invoke()

            return
        }

        val baseContext = context
        val textLayoutContext = MaterialThemeOverlay.wrap(
            baseContext,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle,
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        )

        val layout = TextInputLayout(
            textLayoutContext,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            layoutParams = listItemLayoutParams

            endIconContentDescription = removeStr
            setEndIconOnClickListener { removeItem(index) }

            val layout = this

            addView(
                TextInputEditText(textLayoutContext).apply {
                    layoutParams = listItemLayoutParams
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                    addTextChangedListener {
                        // Updates errorState if necessary
                        val newErrorState = it?.isBlank() != false

                        setErrorState(newErrorState)
                        refreshErrorStateForTextLayout(layout, newErrorState)
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
        removeViewAt(index)
        refreshHintsAndEndButtons()
    }

    private fun setErrorState(value: Boolean) {
        if (isError != value) {
            isError = value
            onErrorStateChanged?.invoke(value)
        }
    }

    fun refreshErrorState() {
        iterateInputs {
            val isError = it.getText().isBlank()

            if (isError) {
                setErrorState(true)
            }

            refreshErrorStateForTextLayout(it, isError)
        }
    }

    private fun refreshErrorStateForTextLayout(layout: TextInputLayout, state: Boolean) {
        layout.error = if (state) {
            emptyTextError
        } else {
            null
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

            val format = getLazyValue(
                meaningAndOrdinalFormat,
                { res.getString(R.string.meaningAndOrdinalFormat) },
                { meaningAndOrdinalFormat = it }
            )

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
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        iterateInputs {
            it.isEnabled = enabled
        }

        // Add button should be invisible when the view is disabled
        getChildAt(childCount - 1).visibility = if(enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun getItems(): Array<out String> {
        val inputCount = childCount - 1 // without add button

        return Array(inputCount) { i ->
            val inputLayout = getTextInputLayoutAt(i)

            inputLayout.getText().trimToString()
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

    companion object {
        private val listItemLayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        private val ADD_BUTTON_STYLE_ATTR =
            com.google.android.material.R.attr.materialButtonOutlinedStyle
    }
}