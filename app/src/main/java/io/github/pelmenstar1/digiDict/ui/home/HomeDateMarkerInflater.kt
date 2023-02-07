package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.MaxRoundRectDrawable
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.time.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY

object HomeDateMarkerInflater : HomePageItemInflater<HomePageItem.DateMarker, HomeDateMarkerInflater.StaticInfo> {
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
        val dateFormatter = CompatDateTimeFormatter(context, DATE_FORMAT)

        val backgroundColor: Int

        val horizontalPadding: Int
        val verticalPadding: Int

        init {
            val res = context.resources
            val theme = context.theme

            horizontalPadding = res.getDimensionPixelOffset(R.dimen.home_dateMarker_horizontalPadding)
            verticalPadding = res.getDimensionPixelOffset(R.dimen.home_dateMarker_verticalPadding)

            backgroundColor = ResourcesCompat.getColor(res, R.color.home_date_marker_background_color, theme)
        }
    }

    override val uniqueId: Int
        get() = 2

    override fun createStaticInfo(context: Context): StaticInfo = StaticInfo(context)

    override fun createView(context: Context, staticInfo: StaticInfo): View {
        return MaterialTextView(context).apply {
            val horizontalPadding = staticInfo.horizontalPadding
            val verticalPadding = staticInfo.verticalPadding

            layoutParams = staticInfo.layoutParams
            background = MaxRoundRectDrawable().also {
                it.style = Paint.Style.FILL
                it.color = staticInfo.backgroundColor
            }

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            staticInfo.textAppearance.apply(this)
        }
    }

    override fun bind(
        view: View,
        item: HomePageItem.DateMarker,
        args: HomePageItemInflaterArgs,
        staticInfo: StaticInfo
    ) {
        view as TextView

        val epochSeconds = item.epochDay * SECONDS_IN_DAY
        view.text = staticInfo.dateFormatter.format(epochSeconds)
    }
}