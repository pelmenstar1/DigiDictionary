package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.utils.decimalDigitCount

class PrefixTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
    @StyleRes defStyleRes: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr, defStyleRes) {
    var prefix: String = ""
        set(value) {
            field = value

            updateText()
        }

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.PrefixTextView, defStyleAttr, defStyleRes)

            try {
                a.getString(R.styleable.PrefixTextView_prefix)?.let {
                    prefix = it
                }
            } finally {
                a.recycle()
            }
        }
    }

    private var textValue: String? = ""
    private var numberValue: Int = 0

    fun setValue(text: String) {
        if (textValue == null || text != textValue) {
            textValue = text

            updateText()
        }
    }

    fun setValue(number: Int) {
        // Update text only if current number is not equals to new number or if text value is set.
        // Also check if text value is set to properly handle situation
        // when setValue(1) is called, then setValue("123") and setValue(1) is called again.
        // (the text must be updated although numberValue is not changed)
        if (textValue != null || numberValue != number) {
            textValue = null
            numberValue = number

            updateText()
        }
    }

    private fun updateText() {
        val prefix = prefix
        val textValue = textValue
        val numberValue = numberValue

        val prefixLength = prefix.length

        if (textValue != null) {
            val buffer = CharArray(prefixLength + textValue.length + 2)
            prefix.toCharArray(buffer, 0)
            buffer[prefixLength] = ':'
            buffer[prefixLength + 1] = ' '
            textValue.toCharArray(buffer, prefixLength + 2)

            setText(buffer, 0, buffer.size)
        } else {
            val capacity = prefixLength +
                    numberValue.decimalDigitCount() +
                    2 /* space and colon */ +
                    if (numberValue < 0) 1 else 0 /* if a number is negative, additional character for - is required */

            val result = buildString(capacity) {
                append(prefix)
                append(':')
                append(' ')
                append(numberValue)
            }

            setText(result, BufferType.NORMAL)
        }
    }
}