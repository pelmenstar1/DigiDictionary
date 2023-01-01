package io.github.pelmenstar1.digiDict.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.MaxRoundRectDrawable

/**
 * Represents a button that looks like a spinner but is actually a button which on click shows dialog with list where
 * the user can select an option.
 */
class RequestListDialogButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle,
    @StyleRes defStyleRes: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {
    var prefix: String = ""
        set(value) {
            field = value

            updateText()
        }

    var value: String = ""
        set(value) {
            field = value

            updateText()
        }

    init {
        val res = context.resources
        val theme = context.theme

        background = MaxRoundRectDrawable().apply {
            color = ResourcesCompat.getColor(res, R.color.request_list_dialog_button_background, theme)
        }
        backgroundTintList = null

        icon = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_down, theme)
        iconGravity = ICON_GRAVITY_END
        iconTint = ResourcesCompat.getColorStateList(res, R.color.request_list_dialog_button_icon_tint, theme)

        val verticalPadding = res.getDimensionPixelOffset(R.dimen.home_requestListDialogButton_verticalPadding)
        val rightPadding = res.getDimensionPixelOffset(R.dimen.home_requestListDialogButton_rightPadding)

        setPadding(0, verticalPadding, rightPadding, verticalPadding)

        minimumHeight = 0
        minHeight = 0

        if (attrs != null) {
            val a =
                context.obtainStyledAttributes(attrs, R.styleable.RequestListDialogButton, defStyleAttr, defStyleRes)

            try {
                a.getString(R.styleable.RequestListDialogButton_prefix)?.let { prefix = it }
            } finally {
                a.recycle()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateText() {
        val prefix = prefix
        val value = value

        val buffer = CharArray(prefix.length + value.length + 2)
        prefix.toCharArray(buffer, destinationOffset = 0)
        buffer[prefix.length] = ':'
        buffer[prefix.length + 1] = ' '
        value.toCharArray(buffer, destinationOffset = prefix.length + 2)

        text = String(buffer)
    }
}