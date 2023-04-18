package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.FixedBitSet
import io.github.pelmenstar1.digiDict.common.android.getParcelableCompat
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.R

/**
 * Represents a base class for a dialog fragment that provides a list of choices and multiple of them can be selected
 */
abstract class MultiSelectionDialogFragment<TValue> : AbstractSelectionDialogFragment() {
    private lateinit var root: LinearLayout
    private lateinit var applyButton: Button
    private lateinit var noOptionSelectedErrorTextView: TextView

    private var selectedStateBitSet: FixedBitSet? = null

    private var noOptionSelectedAnimator: ValueAnimator? = null
    private var noOptionsSelectedAnimationTargetIsVisible: Boolean = false

    /**
     * Gets or sets a callback that will be invoked when a user clicks 'apply' button to confirm that all choices have been made
     */
    var onValuesSelected: ((Array<TValue>) -> Unit)? = null

    /**
     * Gets whether at least one choice should be checked. The default value is `false`.
     *
     * If it's `true`, an error message will be shown if no option is selected.
     */
    open val atLeastOneShouldBeSelected: Boolean
        get() = false

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val res = context.resources

        val choicesArray = choices.get()

        // Saved state has a greater priority because it actually overwrites data in arguments
        if (savedInstanceState != null) {
            initFromSavedState(savedInstanceState)
        } else {
            initFromArgs(choicesArray)
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            val verticalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_rootVerticalPadding)
            val horizontalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_rootHorizontalPadding)

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        root.addView(SelectionDialogFragmentUtils.createTitleView(context, titleRes))
        createAndAddViewsForItems(context, choicesArray, root)

        root.addView(createNoOptionSelectedErrorTextView(context).also {
            noOptionSelectedErrorTextView = it
        })
        root.addView(createApplyButton(context).also {
            applyButton = it
        })

        // To keep the consistent state in case atLeastOneSelected is true and initial selected values array is empty.
        onSelectedStateChanged(showAnimation = false)

        this.root = root

        return ScrollView(context).apply {
            addView(root)
        }
    }

    private fun showNoOptionSelectedError(showAnimation: Boolean) {
        showHideNoOptionsSelectedError(targetVisibility = View.VISIBLE, showAnimation)
    }

    private fun hideNoOptionSelectedError(showAnimation: Boolean) {
        showHideNoOptionsSelectedError(targetVisibility = View.INVISIBLE, showAnimation)
    }

    private fun showHideNoOptionsSelectedError(targetVisibility: Int, showAnimation: Boolean) {
        val errorTextView = noOptionSelectedErrorTextView

        if (showAnimation && errorTextView.visibility != targetVisibility) {
            startNoOptionSelectedVisibilityAnimation(isVisible = targetVisibility == View.VISIBLE)
        } else {
            errorTextView.visibility = targetVisibility
        }
    }

    private fun startNoOptionSelectedVisibilityAnimation(isVisible: Boolean) {
        val errorView = noOptionSelectedErrorTextView

        var animator = noOptionSelectedAnimator
        if (animator == null) {
            val res = requireContext().resources
            val duration = res.getInteger(R.integer.multiSelectionDialog_noOptionSelectedVisiblityAnimationDuration)

            animator = ObjectAnimator()
            animator.duration = duration.toLong()
            animator.setValues(PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f))
            animator.setTarget(errorView)
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    errorView.visibility =
                        if (noOptionsSelectedAnimationTargetIsVisible) View.VISIBLE else View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        } else if (animator.isRunning) {
            animator.cancel()
        }

        noOptionsSelectedAnimationTargetIsVisible = isVisible
        errorView.visibility = View.VISIBLE

        if (isVisible) {
            errorView.alpha = 0f

            animator.start()
        } else {
            errorView.alpha = 1f

            animator.reverse()
        }
    }

    private fun createNoOptionSelectedErrorTextView(context: Context): TextView {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = res.getDimensionPixelOffset(R.dimen.multiSelectionDialog_errorText_marginTop)
                bottomMargin = res.getDimensionPixelOffset(R.dimen.multiSelectionDialog_errorText_marginBottom)
                marginStart = res.getDimensionPixelOffset(R.dimen.multiSelectionDialog_errorText_marginStart)
            }

            text = res.getText(R.string.multiSelectionDialog_noOptionSelectedError)
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_DigiDictionary_MultiSelectionDialog_Error)
        }
    }

    private fun createApplyButton(context: Context): Button {
        val res = context.resources

        return MaterialButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            text = res.getText(R.string.multiSelectionDialog_apply)
            setOnClickListener { apply() }
        }
    }

    private fun createAndAddViewsForItems(context: Context, choices: Array<out String>, root: LinearLayout) {
        val bitSet = selectedStateBitSet!!
        val itemOnCheckedChangedListener = CompoundButton.OnCheckedChangeListener { button, state ->
            val index = button.tag as Int

            bitSet[index] = state
            onSelectedStateChanged(showAnimation = true)
        }

        val res = context.resources

        val textVerticalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textVerticalPadding)
        val textHorizontalPadding = res.getDimensionPixelOffset(R.dimen.selectionDialog_textHorizontalPadding)
        val textAppearance = TextAppearance(context) { BodyLarge }

        val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        for ((index, choice) in choices.withIndex()) {
            root.addView(MaterialCheckBox(context).apply {
                layoutParams = itemLayoutParams
                setPadding(textHorizontalPadding, textVerticalPadding, textHorizontalPadding, textVerticalPadding)

                tag = index
                text = choice
                isChecked = bitSet[index]
                textAppearance.apply(this)

                setOnCheckedChangeListener(itemOnCheckedChangedListener)
            })
        }
    }

    private fun initFromArgs(choicesArray: Array<out String>) {
        requireArguments().getIntArray(ARGS_SELECTED_INDICES)?.also { selectedIndices ->
            // choices must be initialized by this moment
            val bitSet = FixedBitSet(choicesArray.size)
            selectedStateBitSet = bitSet

            for (index in selectedIndices) {
                bitSet.set(index)
            }
        }
    }

    private fun initFromSavedState(savedInstanceState: Bundle) {
        savedInstanceState.getParcelableCompat<FixedBitSet>(STATE_SELECTED_STATE_BIT_SET)?.also { bitSet ->
            selectedStateBitSet = bitSet
        }
    }

    private fun onSelectedStateChanged(showAnimation: Boolean) {
        if (atLeastOneShouldBeSelected) {
            val atLeastOneSelected = selectedStateBitSet!!.countSetBits() > 0

            if (atLeastOneSelected) {
                hideNoOptionSelectedError(showAnimation)
            } else {
                showNoOptionSelectedError(showAnimation)
            }

            applyButton.isEnabled = atLeastOneSelected
        }
    }

    private fun apply() {
        // selectedStateBitSet must be initialized by this moment
        val bitSet = selectedStateBitSet!!

        val selectedValues = createValueArray(bitSet.countSetBits())
        var selectedValueIndex = 0

        bitSet.iterateSetBits { index ->
            selectedValues[selectedValueIndex++] = getValueByIndex(index)
        }

        @Suppress("UNCHECKED_CAST")
        onValuesSelected?.invoke(selectedValues as Array<TValue>)

        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(STATE_SELECTED_STATE_BIT_SET, requireNotNull(selectedStateBitSet))
    }

    /**
     * Returns a [TValue] instance that is represented by an item at specified [index].
     */
    abstract fun getValueByIndex(index: Int): TValue

    /**
     * Creates an empty (consists of nulls) array of [TValue]'s with specified [size].
     */
    abstract fun createValueArray(size: Int): Array<TValue?>

    companion object {
        private const val ARGS_SELECTED_INDICES =
            "io.github.pelmenstar1.digiDict.MultiSelectionDialogFragment.args.selectedIndices"
        private const val STATE_SELECTED_STATE_BIT_SET =
            "io.github.pelmenstar1.digiDict.MultiSelectionDialogFragment.state.selectedStateBitSet"

        /**
         * Creates arguments for the [MultiSelectionDialogFragment] instance.
         * Every instance of [MultiSelectionDialogFragment] must have a non-null arguments value
         */
        fun createArguments(selectedIndices: IntArray): Bundle {
            return Bundle(1).apply {
                putIntArray(ARGS_SELECTED_INDICES, selectedIndices)
            }
        }
    }
}