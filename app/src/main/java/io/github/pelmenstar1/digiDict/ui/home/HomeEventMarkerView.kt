package io.github.pelmenstar1.digiDict.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.getDefaultTextColor
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import io.github.pelmenstar1.digiDict.common.android.getPrimaryColor
import io.github.pelmenstar1.digiDict.common.getLazyValue

class HomeEventMarkerViewStaticInfo(context: Context) {
    val textSize: Float
    val textHorizontalMargin: Float
    val textVerticalMargin: Float

    @ColorInt
    val textColor: Int

    val outlineStrokeWidth: Float

    @ColorInt
    val outlineColor: Int

    init {
        val res = context.resources

        textSize = res.getDimension(R.dimen.home_eventMarker_textSize)
        textHorizontalMargin = res.getDimension(R.dimen.home_eventMarker_textHorizontalMargin)
        textVerticalMargin = res.getDimension(R.dimen.home_eventMarker_textVerticalMargin)
        textColor = context.getDefaultTextColor()

        outlineStrokeWidth = res.getDimension(R.dimen.home_eventMarker_outlineStrokeWidth)
        outlineColor = context.getPrimaryColor()
    }
}

// This view should be created only in HomeEventMarkerInflater
@SuppressLint("ViewConstructor")
class HomeEventMarkerView constructor(
    context: Context,
    private val staticInfo: HomeEventMarkerViewStaticInfo
) : View(context) {
    private var eventStartedFormat: String? = null
    private var eventEndedFormat: String? = null

    private val textPaint: Paint
    private val outlinePaint: Paint

    private var text = ""
    private var textWidth = 0f
    private var textHeight = 0f

    init {
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = staticInfo.textColor
            textSize = staticInfo.textSize
        }

        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = staticInfo.outlineColor
            strokeWidth = staticInfo.outlineStrokeWidth
        }
    }

    fun setContent(isStarted: Boolean, eventName: String) {
        val res = context.resources
        val locale = context.getLocaleCompat()

        val format = if (isStarted) {
            getLazyValue(
                eventStartedFormat,
                { res.getString(R.string.home_eventStartedFormat) },
                { eventStartedFormat = it }
            )
        } else {
            getLazyValue(
                eventEndedFormat,
                { res.getString(R.string.home_eventEndedFormat) },
                { eventEndedFormat = it }
            )
        }

        text = String.format(locale, format, eventName)

        measureText()
        invalidate()
    }

    private fun measureText() {
        val bounds = Rect()
        val text = text

        textPaint.getTextBounds(text, 0, text.length, bounds)

        textWidth = bounds.width().toFloat()
        textHeight = bounds.height().toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (textHeight + staticInfo.textVerticalMargin * 2f + staticInfo.outlineColor + 0.5f).toInt()

        setMeasuredDimension(
            widthMeasureSpec,
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        val w = width.toFloat()
        val h = height.toFloat()

        val tw = textWidth
        val th = textHeight

        c.drawText(text, (w - tw) * 0.5f, h - th, textPaint)
    }
}