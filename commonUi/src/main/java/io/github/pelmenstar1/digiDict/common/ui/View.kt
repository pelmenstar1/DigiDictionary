package io.github.pelmenstar1.digiDict.common.ui

import android.text.PrecomputedText
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.RequiresApi
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.textfield.TextInputLayout
import io.github.pelmenstar1.digiDict.common.equalsByChar
import io.github.pelmenstar1.digiDict.common.textAppearance.MaterialTextAppearanceSelector

private const val TAG = "ViewKt"

fun TextInputLayout.setText(text: CharSequence) {
    editText?.setText(text)
}

fun EditText.setTextIfCharsChanged(newText: CharSequence) {
    if (!text.equalsByChar(newText)) {
        setText(newText)
    }
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
    adjustViewCountInternal(targetCount, childCount, addNewView)
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


fun <T : View> ViewGroup.getTypedViewAt(index: Int) =
    getNullableTypedViewAt<T>(index) ?: throw NullPointerException("The view at index $index doesn't exist")

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.getNullableTypedViewAt(index: Int) = getChildAt(index) as T?

fun View.setPaddingRes(@DimenRes resId: Int) {
    setPadding(resources.getDimensionPixelOffset(resId))
}

inline fun TextView.setTextAppearance(block: MaterialTextAppearanceSelector.() -> Int) {
    TextViewCompat.setTextAppearance(this, MaterialTextAppearanceSelector.block())
}

/**
 * Attempts to set [TextView]'s text. If the operation succeeds, the method returns true, otherwise false.
 *
 * [TextView.setText] may fail if text is of type [PrecomputedText] and an instance of [PrecomputedText] is created
 * for wrong [TextView]'s properties.
 */
@RequiresApi(28)
fun TextView.trySetPrecomputedText(precomputed: PrecomputedText): Boolean {
    return try {
        text = precomputed

        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set precomputed text. Falling back to simple text", e)

        false
    }
}