package io.github.pelmenstar1.digiDict.utils

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

inline fun TextInputLayout.addTextChangedListener(crossinline block: (CharSequence) -> Unit) {
    editText?.addTextChangedListener {
        block(it ?: "")
    }
}