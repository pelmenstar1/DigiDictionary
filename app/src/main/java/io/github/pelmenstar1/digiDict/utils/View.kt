package io.github.pelmenstar1.digiDict.utils

import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.getText(): CharSequence {
    return editText?.text ?: ""
}

fun TextInputLayout.setText(text: CharSequence) {
    editText?.setText(text)
}

fun ViewGroup.setOnClickListenerWithDescendants(listener: View.OnClickListener?) {
    setOnClickListener(listener)

    for(i in 0 until childCount) {
        getChildAt(i).setOnClickListener(listener)
    }
}