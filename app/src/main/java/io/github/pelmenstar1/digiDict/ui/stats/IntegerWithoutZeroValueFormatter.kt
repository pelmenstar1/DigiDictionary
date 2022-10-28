package io.github.pelmenstar1.digiDict.ui.stats

import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Represents a value formatter that truncates given [Float] value to [Int] and if that [Int] is zero, returns an empty string,
 * otherwise converts the [Int] to [String] using simple [Int.toString] method
 */
object IntegerWithoutZeroValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val iValue = value.toInt()

        return if (iValue == 0) "" else iValue.toString()
    }
}