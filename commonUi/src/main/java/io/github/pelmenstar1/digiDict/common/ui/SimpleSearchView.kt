package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText

class SimpleSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
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