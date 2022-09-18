package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import io.github.pelmenstar1.digiDict.common.textAppearance.MaterialTextAppearanceSelector

fun TextInputLayout.setText(text: CharSequence) {
    editText?.setText(text)
}

inline fun TextInputLayout.addTextChangedListener(crossinline block: (CharSequence) -> Unit) {
    editText?.addTextChangedListener { block(it ?: "") }
}

inline fun <TGroup : ViewGroup> TGroup.adjustViewCountWithoutLast(
    targetCount: Int,
    lastViewsCount: Int,
    addNewView: TGroup.() -> Unit
) {
    adjustViewCountInternal(targetCount, childCount - lastViewsCount, addNewView)
}

inline fun <TGroup : ViewGroup> TGroup.adjustViewCount(
    targetCount: Int,
    addNewView: TGroup.() -> Unit
) {
    val childCount = childCount
    if (childCount == 0) {
        removeAllViews()
    } else {
        adjustViewCountInternal(targetCount, childCount, addNewView)
    }
}

inline fun <TGroup : ViewGroup> TGroup.adjustViewCountInternal(
    targetCount: Int,
    currentCount: Int,
    addNewView: TGroup.() -> Unit
) {
    when {
        currentCount > targetCount -> {
            removeViews(targetCount, currentCount - targetCount)
        }
        currentCount < targetCount -> {
            repeat(targetCount - currentCount) { addNewView() }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.getTypedViewAt(index: Int) = getChildAt(index) as T

fun View.setPaddingRes(@DimenRes resId: Int) {
    setPadding(resources.getDimensionPixelOffset(resId))
}

inline fun TextView.setTextAppearance(block: MaterialTextAppearanceSelector.() -> Int) {
    TextViewCompat.setTextAppearance(this, MaterialTextAppearanceSelector.block())
}