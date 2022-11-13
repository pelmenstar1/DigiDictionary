package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY

object HomeDateMarkerHelper {
    private const val DATE_FORMAT = "dd MMMM yyyy"

    class StaticInfo(context: Context) {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val textAppearance = TextAppearance(context) { BodyLarge }
        val padding: Int
        val background: Drawable
        val dateFormatter = CompatDateTimeFormatter(context, DATE_FORMAT)

        init {
            val res = context.resources

            padding = res.getDimensionPixelOffset(R.dimen.home_dateMarker_padding)
            background = ResourcesCompat.getDrawable(res, R.drawable.home_date_marker_background, context.theme)!!
        }
    }

    fun createView(context: Context, staticInfo: StaticInfo): TextView {
        return MaterialTextView(context).apply {
            layoutParams = staticInfo.layoutParams
            background = staticInfo.background

            setPadding(staticInfo.padding)
            staticInfo.textAppearance.apply(this)
        }
    }

    fun bind(view: TextView, epochDay: Long, staticInfo: StaticInfo) {
        val epochSeconds = epochDay * SECONDS_IN_DAY

        view.text = staticInfo.dateFormatter.format(epochSeconds)
    }
}