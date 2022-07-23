package io.github.pelmenstar1.digiDict.utils

import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setText(text: CharSequence) {
    editText?.setText(text)
}

inline fun TextInputLayout.addTextChangedListenerToString(crossinline block: (String) -> Unit) {
    editText?.addTextChangedListener {
        block(it?.toString() ?: "")
    }
}

fun TextView.setFormattedText(format: String, vararg args: Any?) {
    text = JvmUtils.format(context.getLocaleCompat(), format, args)
}

inline fun TextInputLayout.addTextChangedListener(crossinline block: (CharSequence) -> Unit) {
    editText?.addTextChangedListener {
        block(it ?: "")
    }
}