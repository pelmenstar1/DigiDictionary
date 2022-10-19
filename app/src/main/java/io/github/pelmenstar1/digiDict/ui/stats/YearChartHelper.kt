package io.github.pelmenstar1.digiDict.ui.stats

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLocaleCompat
import io.github.pelmenstar1.digiDict.stats.MonthAdditionStats
import java.text.DateFormatSymbols

object YearChartHelper {
    private class ChartOptions(
        @ColorInt val textColor: Int,
        @ColorInt val minColor: Int,
        @ColorInt val maxColor: Int,
        @ColorInt val avgColor: Int,
        val minLabel: String,
        val maxLabel: String,
        val avgLabel: String
    )

    private class MonthValueFormatter(context: Context) : ValueFormatter() {
        private val months = DateFormatSymbols.getInstance(context.getLocaleCompat()).shortMonths

        override fun getFormattedValue(value: Float): String {
            return months[value.toInt()]
        }
    }

    private fun createChartOptions(context: Context): ChartOptions {
        val res = context.resources
        val theme = context.theme

        val textColor = ResourcesCompat.getColor(res, R.color.stats_chart_text_color, theme)
        val minColor = ResourcesCompat.getColor(res, R.color.stats_chart_min_color, theme)
        val maxColor = ResourcesCompat.getColor(res, R.color.stats_chart_max_color, theme)
        val avgColor = ResourcesCompat.getColor(res, R.color.stats_chart_avg_color, theme)

        val minLabel = res.getString(R.string.stats_chart_minLabel)
        val maxLabel = res.getString(R.string.stats_chart_maxLabel)
        val avgLabel = res.getString(R.string.stats_chart_avgLabel)

        return ChartOptions(textColor, minColor, maxColor, avgColor, minLabel, maxLabel, avgLabel)
    }

    private fun createChartData(rawData: Array<out MonthAdditionStats>, options: ChartOptions): LineData {
        return LineData(
            createChartDataSet(rawData, options, { min.toFloat() }, { minLabel }, { minColor }),
            createChartDataSet(rawData, options, { max.toFloat() }, { maxLabel }, { maxColor }),
            createChartDataSet(rawData, options, { average }, { avgLabel }, { avgColor })
        ).apply {
        }
    }

    private fun LineDataSet.setupColors(dataColor: Int, textColor: Int) {
        color = dataColor
        setCircleColor(dataColor)
        circleHoleColor = dataColor
        valueTextColor = textColor
    }

    private inline fun createChartDataSet(
        rawData: Array<out MonthAdditionStats>,
        options: ChartOptions,
        dataSelector: MonthAdditionStats.() -> Float,
        labelSelector: ChartOptions.() -> String,
        colorSelector: ChartOptions.() -> Int
    ): LineDataSet {
        val entries = ArrayList<Entry>(12)

        for (i in 0 until 12) {
            val entry = Entry(i.toFloat(), rawData[i].dataSelector())

            entries.add(entry)
        }

        return LineDataSet(entries, options.labelSelector()).apply {
            valueFormatter = IntegerWithoutZeroValueFormatter

            setupColors(dataColor = options.colorSelector(), options.textColor)
        }
    }

    fun setupChart(chart: LineChart, rawData: Array<out MonthAdditionStats>) {
        val context = chart.context
        val options = createChartOptions(context)

        chart.run {
            xAxis.apply {
                granularity = 1f
                valueFormatter = MonthValueFormatter(context)
                textColor = options.textColor
            }

            axisLeft.apply {
                textColor = options.textColor
            }

            axisRight.isEnabled = false

            legend.textColor = options.textColor
            description.isEnabled = false

            isHighlightPerTapEnabled = false
            isHighlightPerDragEnabled = false

            data = createChartData(rawData, options)
        }
    }
}