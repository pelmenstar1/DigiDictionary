package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.content.Context
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R

// Instantiated only from the code.
class BadgeView(context: Context) : LinearLayout(context) {
    private val textView: TextView
    private val removeButton: Button

    var text: String = ""
        set(value) {
            field = value

            textView.text = value
        }

    init {
        val res = context.resources
        val theme = context.theme

        orientation = HORIZONTAL
        background = ResourcesCompat.getDrawable(res, R.drawable.badge_view_bg, theme)

        val horizontalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingHorizontal)
        val verticalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingVertical)

        setPadding(
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )

        addView(MaterialTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )

            textView = this
        })

        addView(MaterialButton(context, null, R.attr.materialButtonIconStyle).apply {
            val size = res.getDimensionPixelSize(R.dimen.badge_removeButton_size)

            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL

                marginStart = res.getDimensionPixelOffset(R.dimen.badge_removeButton_startMargin)
            }

            setPadding(0)
            insetTop = 0
            insetBottom = 0

            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            setIconResource(R.drawable.ic_remove)
            setIconTintResource(R.color.badge_icon_tint)

            removeButton = this
        })
    }

    fun setOnRemoveListener(listener: OnClickListener) {
        removeButton.setOnClickListener(listener)
    }
}