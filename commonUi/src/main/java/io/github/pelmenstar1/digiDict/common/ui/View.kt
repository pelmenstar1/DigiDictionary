package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
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

inline fun <TGroup : ViewGroup> TGroup.adjustViewCount(
    targetCount: Int,
    lastViewsCount: Int = 0,
    addNewView: TGroup.() -> Unit
) {
    val currentCount = childCount - lastViewsCount

    when {
        currentCount > targetCount -> {
            removeViews(currentCount, targetCount - currentCount)
        }
        currentCount < targetCount -> {
            repeat(targetCount - currentCount) { addNewView() }
        }
    }
}

// TODO: Use it in more places.
@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.getTypedViewAt(index: Int) = getChildAt(index) as T