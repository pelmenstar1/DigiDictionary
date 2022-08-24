package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

// Instantiated only from the code.
class BadgeWithRemoveButtonView(context: Context) : LinearLayout(context) {
    private val textView: TextView
    private val removeButton: Button

    private val outlineHelper = BadgeOutlineHelper(context)
    private var _badge: RecordBadgeInfo? = null

    var badge: RecordBadgeInfo
        get() = requireNotNull(_badge)
        set(value) {
            if (_badge != value) {
                _badge = value

                textView.text = value.name
                outlineHelper.setOutlineColor(value.outlineColor)

                invalidate()
            }
        }

    init {
        orientation = HORIZONTAL
        setWillNotDraw(false)

        val res = context.resources
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

            // TODO: Generalize TextViewCompat.setTextAppearance(..., _BodyLarge)
            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )

            ellipsize = null
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        outlineHelper.onSizeChanged(w, h)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        outlineHelper.draw(c)
    }
}