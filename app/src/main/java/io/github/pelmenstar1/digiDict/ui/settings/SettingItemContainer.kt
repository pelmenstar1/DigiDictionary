package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R

class SettingItemContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    init {
        orientation = HORIZONTAL

        val res = context.resources
        val padding = res.getDimensionPixelOffset(R.dimen.settingItemContainer_padding)

        setPadding(padding)

        val imageView: AppCompatImageView
        val nameView: MaterialTextView

        super.addView(AppCompatImageView(context).apply {
            val size = res.getDimensionPixelSize(R.dimen.settingItemContainer_iconSize)

            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            imageView = this
        })

        super.addView(MaterialTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL

                marginStart = res.getDimensionPixelOffset(R.dimen.settingItemContainer_nameMarginStart)
                weight = 1f
            }

            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )

            nameView = this
        })

        if (attrs != null) {
            val theme = context.theme
            val a = theme.obtainStyledAttributes(attrs, R.styleable.SettingItemContainer, defStyleAttr, defStyleRes)

            try {
                if (a.hasValue(R.styleable.SettingItemContainer_icon)) {
                    val resId = a.getResourceId(R.styleable.SettingItemContainer_icon, 0)
                    val icon = ResourcesCompat.getDrawable(res, resId, theme)

                    imageView.setImageDrawable(icon)
                }

                a.getString(R.styleable.SettingItemContainer_name)?.let {
                    nameView.text = it
                }
            } finally {
                a.recycle()
            }
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (params is LayoutParams) {
            params.gravity = Gravity.CENTER_VERTICAL
        }

        super.addView(child, index, params)
    }
}