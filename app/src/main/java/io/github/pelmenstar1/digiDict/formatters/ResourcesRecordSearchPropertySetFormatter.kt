package io.github.pelmenstar1.digiDict.formatters

import android.content.res.Resources
import androidx.annotation.StringRes
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * An implementation of [RecordSearchPropertySetFormatter] that uses Android resources
 * as a way to format a set of [RecordSearchProperty]
 */
class ResourcesRecordSearchPropertySetFormatter(private val res: Resources) : RecordSearchPropertySetFormatter {
    // Stores the cached values of corresponding resource values.
    // A value for enum is located at its ordinal number + 1
    // The first value is a string for "all".
    private val cache = AtomicReferenceArray<String>(getEnumFieldCount<RecordSearchProperty>() + 1)

    private inline fun getFromCacheOrQuery(index: Int, getId: () -> Int): String {
        val cachedValue = cache.get(index)

        if (cachedValue != null) {
            return cachedValue
        }

        val resValue = res.getString(getId())
        cache.set(index, resValue)

        return resValue
    }

    @StringRes
    private fun getResId(value: RecordSearchProperty) = when (value) {
        RecordSearchProperty.EXPRESSION -> R.string.expression
        RecordSearchProperty.MEANING -> R.string.meaning
    }

    private fun getAllString(): String {
        return getFromCacheOrQuery(index = 0) { R.string.home_searchProperty_all }
    }

    private fun getString(value: RecordSearchProperty): String {
        return getFromCacheOrQuery(value.ordinal + 1) { getResId(value) }
    }

    override fun format(values: Array<out RecordSearchProperty>): String {
        val hasExpression = values.contains(RecordSearchProperty.EXPRESSION)
        val hasMeaning = values.contains(RecordSearchProperty.MEANING)

        return when {
            hasExpression && hasMeaning -> getAllString()
            hasExpression -> getString(RecordSearchProperty.EXPRESSION)
            hasMeaning -> getString(RecordSearchProperty.MEANING)
            else -> ""
        }
    }
}