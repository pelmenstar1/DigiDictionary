package io.github.pelmenstar1.digiDict.ui.stats

import com.github.mikephil.charting.formatter.ValueFormatter

object IntegerWithoutZeroValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val iValue = value.toInt()

        return if (iValue == 0) "" else iValue.toString()
    }
}