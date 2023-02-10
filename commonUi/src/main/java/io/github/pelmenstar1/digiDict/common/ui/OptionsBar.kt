package io.github.pelmenstar1.digiDict.common.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import io.github.pelmenstar1.digiDict.common.android.MaxRoundRectDrawable
import io.github.pelmenstar1.digiDict.common.android.getColorSurfaceVariant
import io.github.pelmenstar1.digiDict.common.equalsPattern

/**
 * A scrollable bar with options.
 *
 * All the options are represented by buttons that are alike to dropdown - that's done on purpose.
 */
class OptionsBar : HorizontalScrollView {
    /**
     * Stores the information about an option for [OptionsBar].
     *
     * @param id the id of the option. It must be unique among the [Preset]'s
     * @param prefixRes the string resource of prefix of the option
     */
    data class Option(
        @IdRes val id: Int,
        @StringRes val prefixRes: Int
    )

    /**
     * Stores the information about preset for [OptionsBar].
     *
     * @param options the options for this preset. The ids of [options] must be unique although it's not validated
     */
    data class Preset(val options: Array<out Option>) {
        override fun equals(other: Any?) = equalsPattern(other) { o ->
            options.contentEquals(o.options)
        }

        override fun hashCode(): Int {
            return options.contentHashCode()
        }

        override fun toString(): String {
            return "Preset(options=${options.contentToString()})"
        }
    }

    private class OptionButton(context: Context) : MaterialButton(context) {
        private var _prefix: String = ""
        private var _value: String = ""

        var value: String
            get() = _value
            set(value) {
                _value = value

                updateText()
            }

        init {
            val res = context.resources
            val theme = context.theme

            background = MaxRoundRectDrawable().apply {
                color = context.getColorSurfaceVariant()
            }
            backgroundTintList = null

            icon = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_down, theme)
            iconGravity = ICON_GRAVITY_END
            iconTint = ResourcesCompat.getColorStateList(res, R.color.options_bar_button_icon_tint, theme)

            val verticalPadding = res.getDimensionPixelOffset(R.dimen.optionsBarButton_verticalPadding)
            val startPadding = res.getDimensionPixelOffset(R.dimen.optionsBarButton_startPadding)
            val endPadding = res.getDimensionPixelOffset(R.dimen.optionsBarButton_endPadding)

            setPaddingRelative(startPadding, verticalPadding, endPadding, verticalPadding)

            minimumHeight = 0
            minHeight = 0
        }

        fun setPrefixAndValue(prefix: String, value: String) {
            _prefix = prefix
            _value = value

            updateText()
        }

        @SuppressLint("SetTextI18n")
        private fun updateText() {
            val prefix = _prefix
            val value = _value

            val buffer = CharArray(prefix.length + value.length + 2)
            prefix.toCharArray(buffer, destinationOffset = 0)
            buffer[prefix.length] = ':'
            buffer[prefix.length + 1] = ' '
            value.toCharArray(buffer, destinationOffset = prefix.length + 2)

            text = String(buffer)
        }
    }

    private lateinit var optionsContainer: LinearLayout

    private val values = SparseArray<String>()
    private val onClickListeners = SparseArray<OnClickListener>()

    private lateinit var optionButtonLayoutParams: LinearLayout.LayoutParams

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        val res = context.resources

        isHorizontalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = false

        optionButtonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = res.getDimensionPixelOffset(R.dimen.optionsBarButton_endMargin)
        }

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL

            optionsContainer = this
        })
    }

    /**
     * Changes the set of options for the options bar.
     * The state of options will be retained between presets, including on click listeners
     */
    fun setPreset(preset: Preset) {
        val options = preset.options
        val container = optionsContainer

        container.adjustViewCount(options.size) {
            addView(createButton())
        }

        for (i in options.indices) {
            val button = container.getTypedViewAt<OptionButton>(i)

            bindButton(button, options[i])
        }
    }

    /**
     * Sets the on click listener for the option specified by its [id].
     */
    fun setOptionOnClickListener(@IdRes id: Int, listener: OnClickListener) {
        onClickListeners[id] = listener

        findOptionButton(id)?.setOnClickListener(listener)
    }

    /**
     * Sets the [value] for the option specified by its [id].
     */
    fun setOptionValue(@IdRes id: Int, value: String) {
        values[id] = value

        findOptionButton(id)?.value = value
    }

    private fun findOptionButton(@IdRes id: Int): OptionButton? {
        return optionsContainer.findViewById(id)
    }

    private fun createButton(): OptionButton {
        return OptionButton(context).apply {
            layoutParams = optionButtonLayoutParams
        }
    }

    private fun bindButton(button: OptionButton, option: Option) {
        val id = option.id
        val prefix = resources.getString(option.prefixRes)

        button.id = id
        button.setPrefixAndValue(prefix, values.get(id, ""))
        button.setOnClickListener(onClickListeners[id])
    }

    companion object {
        /**
         * Creates the instance of [Preset] in more readable manner using varargs.
         */
        fun Preset(vararg options: Option): Preset {
            return Preset(options)
        }
    }
}