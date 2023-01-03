package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.time.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY

object HomeDateMarkerHelper {
    private const val DATE_FORMAT = "dd MMMM yyyy"

    class StaticInfo(context: Context) {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val verticalMargin = context.resources.getDimensionPixelOffset(R.dimen.home_dateMarker_verticalMargin)

            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = verticalMargin
            bottomMargin = verticalMargin
        }

        val textAppearance = TextAppearance(context, R.style.TextAppearance_DigiDictionary_Home_DateMarker)
        val background: Drawable
        val dateFormatter = CompatDateTimeFormatter(context, DATE_FORMAT)

        val horizontalPadding: Int
        val verticalPadding: Int

        init {
            val res = context.resources

            horizontalPadding = res.getDimensionPixelOffset(R.dimen.home_dateMarker_horizontalPadding)
            verticalPadding = res.getDimensionPixelOffset(R.dimen.home_dateMarker_verticalPadding)

            // TODO: Don't make Drawable shared across views
            background = ResourcesCompat.getDrawable(res, R.drawable.home_date_marker_background, context.theme)!!
        }
    }

    fun createView(context: Context, staticInfo: StaticInfo): TextView {
        return MaterialTextView(context).apply {
            val horizontalPadding = staticInfo.horizontalPadding
            val verticalPadding = staticInfo.verticalPadding

            layoutParams = staticInfo.layoutParams
            background = staticInfo.background

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            staticInfo.textAppearance.apply(this)
        }
    }

    fun bind(view: TextView, epochDay: Long, staticInfo: StaticInfo) {
        val epochSeconds = epochDay * SECONDS_IN_DAY

        view.text = staticInfo.dateFormatter.format(epochSeconds)
    }
}