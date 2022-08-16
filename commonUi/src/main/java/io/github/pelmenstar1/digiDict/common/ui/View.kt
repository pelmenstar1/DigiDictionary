package io.github.pelmenstar1.digiDict.common.ui

import android.view.ViewGroup
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

inline fun ViewGroup.adjustViewCount(targetCount: Int, lastViewsCount: Int, addView: () -> Unit) {
    val currentCount = childCount - lastViewsCount

    when {
        currentCount > targetCount -> {
            removeViews(currentCount, targetCount - currentCount)
        }
        currentCount < targetCount -> {
            repeat(targetCount - currentCount) { addView() }
        }
    }
}