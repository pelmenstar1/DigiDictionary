package io.github.pelmenstar1.digiDict.ui.manageEvents

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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
import io.github.pelmenstar1.digiDict.common.android.InfiniteLoopAnimator
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.lerpRgb
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

    private val dividerPaint: Paint
    private val dividerStrokeWidth: Float

    private val outlineAnimator: InfiniteLoopAnimator
    private val outlinePaint: Paint
    private val outlineAnimationStartColor: Int
    private val outlineAnimationEndColor: Int
    private val outlineStrokeWidth: Float
    private val outlineRoundRadius: Float
    private var isOutlineAnimationEndedOnDetach = false

    /**
     * Gets or sets whether the container is located before the event that is not ended.
     */
    var isBeforeEventNotEnded: Boolean = false
        set(value) {
            field = value

            invalidate()
        }

    /**
     * Gets or sets whether the event is not ended.
     */
    var isEventNotEnded: Boolean = false
        set(value) {
            // View handling below expects that the value is actually changed.
            if (field != value) {
                field = value

                if (value) {
                    val button = getLazyValue(stopEventButton, ::createStopEventButton) { stopEventButton = it }
                    button.setOnClickListener(stopEventListener)

                    outlineAnimator.start()

                    addView(button)
                } else {
                    outlineAnimator.stop()

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
        setWillNotDraw(false)

        val paddingBottom = res.getDimensionPixelOffset(R.dimen.manageEvents_itemPaddingBottom)
        setPadding(0, 0, 0, paddingBottom)

        outlineStrokeWidth = res.getDimension(R.dimen.manageEvents_itemOutlineStrokeWidth)
        outlineRoundRadius = res.getDimension(R.dimen.manageEvents_itemOutlineRoundRadius)

        outlineAnimationStartColor =
            ResourcesCompat.getColor(res, R.color.manage_event_item_container_animation_start, theme)
        outlineAnimationEndColor =
            ResourcesCompat.getColor(res, R.color.manage_event_item_container_animation_end, theme)

        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = outlineStrokeWidth
        }

        outlineAnimator = InfiniteLoopAnimator(::onOutlineTransitionTick).apply {
            duration = res.getInteger(R.integer.manageEventItem_outlineTransitionDuration).toLong()
        }

        dividerStrokeWidth = res.getDimension(R.dimen.manageEvents_itemDividerStrokeWidth)
        dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ResourcesCompat.getColor(res, R.color.manage_event_divider_color, theme)
            strokeWidth = dividerStrokeWidth
        }

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (isOutlineAnimationEndedOnDetach) {
            isOutlineAnimationEndedOnDetach = false
            outlineAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        outlineAnimator.stop()
        isOutlineAnimationEndedOnDetach = true
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

    private fun onOutlineTransitionTick(fraction: Float) {
        val color = lerpRgb(outlineAnimationStartColor, outlineAnimationEndColor, fraction)

        outlinePaint.color = color
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        val width = width.toFloat()
        val height = height.toFloat()

        // If isEventNotEnded is true, the outline is drawn.
        if (isEventNotEnded) {
            val hsw = outlineStrokeWidth * 0.5f
            val rr = outlineRoundRadius

            c.drawRoundRect(
                hsw, hsw,
                width - hsw, height - hsw,
                rr, rr,
                outlinePaint
            )
        }

        // In case if either isEventNotEnded or isBeforeEventNotEnded are true, the divider would look ugly.
        if (!isEventNotEnded && !isBeforeEventNotEnded) {
            c.drawRect(
                0f, height - dividerStrokeWidth,
                width, height,
                dividerPaint
            )
        }
    }

    companion object {
        private const val REMOVE_BUTTON_INDEX = 1
    }
}