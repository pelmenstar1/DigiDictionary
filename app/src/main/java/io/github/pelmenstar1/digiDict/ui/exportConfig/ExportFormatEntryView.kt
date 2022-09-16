package io.github.pelmenstar1.digiDict.ui.exportConfig

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance

class ExportFormatEntryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    var isChecked: Boolean = false
        set(value) {
            field = value

            indicatorView.visibility = if (value) VISIBLE else INVISIBLE
        }

    var name: CharSequence = ""
        set(value) {
            field = value

            nameView.text = value
        }

    var description: CharSequence = ""
        set(value) {
            field = value

            descriptionView.text = value
        }

    private val indicatorView: ImageView
    private val textViewContainer: LinearLayout
    private val nameView: TextView
    private val descriptionView: TextView

    init {
        val res = context.resources

        orientation = HORIZONTAL
        setPaddingRes(R.dimen.exportFormatEntry_padding)

        indicatorView = ImageView(context).apply {
            val size = res.getDimensionPixelSize(R.dimen.exportFormatEntry_indicatorSize)

            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = res.getDimensionPixelOffset(R.dimen.exportFormatEntry_indicatorMarginEnd)
            }

            setImageResource(R.drawable.ic_selected)
            visibility = INVISIBLE
        }

        textViewContainer = LinearLayout(context).apply {
            orientation = VERTICAL

            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            nameView = MaterialTextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = res.getDimensionPixelOffset(R.dimen.exportFormatEntry_nameMarginBottom)
                }

                setTextAppearance { BodyLarge }
                setTypeface(typeface, Typeface.BOLD)
            }

            descriptionView = MaterialTextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

                setTextAppearance { BodyMedium }
            }

            addView(nameView)
            addView(descriptionView)
        }

        addView(indicatorView)
        addView(textViewContainer)
    }
}