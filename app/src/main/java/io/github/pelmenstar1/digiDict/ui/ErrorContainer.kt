package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R

class ErrorContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val errorTextView: TextView
    private val retryButton: Button

    init {
        val res = context.resources

        orientation = VERTICAL

        addView(MaterialTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                val hMargin = res.getDimensionPixelOffset(R.dimen.errorContainer_textHorizontalMargin)

                leftMargin = hMargin
                rightMargin = hMargin

                gravity = Gravity.CENTER_HORIZONTAL
            }

            errorTextView = this
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )
        })

        addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = res.getDimensionPixelOffset(R.dimen.errorContainer_retryTopMargin)

                gravity = Gravity.CENTER_HORIZONTAL
            }

            text = res.getText(R.string.retry)
            retryButton = this
        })

        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.ErrorContainer, defStyleAttr, defStyleRes)

            try {
                array.getText(R.styleable.ErrorContainer_errorText)?.let {
                    setErrorText(it)
                }
            } finally {
                array.recycle()
            }
        }
    }

    fun setErrorText(text: CharSequence) {
        errorTextView.text = text
    }

    fun setOnRetryListener(listener: OnClickListener) {
        retryButton.setOnClickListener(listener)
    }

    inline fun setOnRetryListener(crossinline block: () -> Unit) {
        setOnRetryListener(OnClickListener { block() })
    }
}