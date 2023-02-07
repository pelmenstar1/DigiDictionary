package io.github.pelmenstar1.digiDict.ui.manageEvents

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance

class ManageEventItemContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val nameView: TextView
    private var stopEventButton: Button? = null
    private var stopEventListener: OnClickListener? = null

    var isEventNotEnded: Boolean = false
        set(value) {
            // View handling below expects that the value is actually changed.
            if (field != value) {
                field = value

                if (value) {
                    val button = getLazyValue(stopEventButton, ::createStopEventButton) { stopEventButton = it }
                    button.setOnClickListener(stopEventListener)

                    addView(button)
                } else {
                    removeViewAt(REMOVE_BUTTON_INDEX)
                }
            }
        }

    var onExecuteAction: ((itemId: Int, actionId: Int) -> Unit)? = null

    var name: String = ""
        set(value) {
            field = value

            nameView.text = value
        }

    var eventId: Int = -1

    init {
        val res = context.resources
        val theme = context.theme

        orientation = VERTICAL

        val paddingBottom = res.getDimensionPixelOffset(R.dimen.manageEvents_itemPaddingBottom)
        setPadding(0, 0, 0, paddingBottom)

        addView(LinearLayout(context).apply {
            val paddingStartTop = res.getDimensionPixelOffset(R.dimen.manageEvents_itemMainContainerStartTopPadding)

            setPaddingRelative(paddingStartTop, paddingStartTop, 0, 0)

            addView(MaterialTextView(context).apply {
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT
                ).also {
                    it.weight = 1f
                    it.gravity = Gravity.CENTER_VERTICAL
                }

                setTextAppearance { BodyLarge }
                nameView = this
            })

            addView(MaterialButton(context, null, R.attr.materialButtonIconStyle).apply {
                val size = res.getDimensionPixelSize(R.dimen.manageEvents_itemMenuIconSize)
                layoutParams = LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }

                icon = ResourcesCompat.getDrawable(res, R.drawable.ic_more, theme)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 0

                setOnClickListener {
                    val menu = PopupMenu(context, it)
                    menu.inflate(R.menu.manage_events)
                    menu.gravity = Gravity.TOP
                    menu.setOnMenuItemClickListener { item ->
                        onExecuteAction?.invoke(eventId, item.itemId)

                        true
                    }
                    menu.show()
                }
            })
        })
    }

    private fun createStopEventButton(): Button {
        val res = resources

        return MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL

                topMargin = res.getDimensionPixelOffset(R.dimen.manageEvents_itemStopButtonMarginTop)
            }

            text = res.getText(R.string.manageEvents_stopEvent)
        }
    }

    fun setStopEventListener(listener: OnClickListener) {
        stopEventListener = listener
        stopEventButton?.setOnClickListener(listener)
    }

    companion object {
        private const val REMOVE_BUTTON_INDEX = 1
    }
}