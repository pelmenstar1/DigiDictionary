package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.TransparentDrawable

class HomeSearchEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    init {
        hint = context.resources.getText(R.string.search)
        background = TransparentDrawable
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        // Looks like it's the only way to make the EditText fill all the space in a toolbar.
        //
        // When SimpleSearchView is inflated in the toolbar, setLayoutParams() is called with LayoutParams
        // whose width and height are WRAP_CONTENT. By overloading setLayoutParams(), LayoutParams can be intercepted.
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT

        super.setLayoutParams(params)
    }
}