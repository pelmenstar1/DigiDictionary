package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import io.github.pelmenstar1.digiDict.R

class HomeSearchEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    init {
        hint = context.resources.getText(R.string.search)
        background = null
    }
}