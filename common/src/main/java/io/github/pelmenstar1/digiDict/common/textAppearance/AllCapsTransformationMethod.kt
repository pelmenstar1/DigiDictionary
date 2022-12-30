package io.github.pelmenstar1.digiDict.common.textAppearance

import android.content.Context
import android.graphics.Rect
import android.icu.text.CaseMap
import android.icu.text.Edits
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.TransformationMethod
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import java.util.*

class AllCapsTransformationMethod(context: Context) : TransformationMethod {
    private val contextLocale = context.getLocaleCompat()

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence? {
        if (source == null) {
            return null
        }

        var locale: Locale? = null
        if (view is TextView) {
            locale = view.textLocale
        }

        if (locale == null) {
            locale = contextLocale
        }

        return if (Build.VERSION.SDK_INT >= 29) {
            val copySpans = source is Spanned

            toUpperCaseApi29(locale, source, copySpans)
        } else {
            source.toString().uppercase(contextLocale)
        }
    }

    override fun onFocusChanged(
        view: View?, sourceText: CharSequence?, focused: Boolean,
        direction: Int, previouslyFocusedRect: Rect?
    ) {
    }

    companion object {
        @RequiresApi(29)
        internal fun toUpperCaseApi29(locale: Locale, source: CharSequence, copySpans: Boolean): CharSequence {
            //
            // The code was taken from android.text.TextUtils
            //

            val edits = Edits()
            val caseMap = CaseMap.toUpper()

            if (!copySpans) { // No spans. Just uppercase the characters.
                val result = caseMap.apply(locale, source, StringBuilder(), edits)
                return if (edits.hasChanges()) result else source
            }

            val result = caseMap.apply(locale, source, SpannableStringBuilder(), edits)

            if (!edits.hasChanges()) {
                // No changes happened while capitalizing. We can return the source as it was.
                return source
            }

            val iterator = edits.fineIterator
            val sourceLength = source.length
            val spanned = source as Spanned
            val spans = spanned.getSpans(0, sourceLength, Any::class.java)

            for (span in spans) {
                val sourceStart = spanned.getSpanStart(span)
                val sourceEnd = spanned.getSpanEnd(span)
                val flags = spanned.getSpanFlags(span)

                // Make sure the indices are not at the end of the string, since in that case
                // iterator.findSourceIndex() would fail.
                val destStart =
                    if (sourceStart == sourceLength) result.length else toUpperMapToDest(iterator, sourceStart)
                val destEnd = if (sourceEnd == sourceLength) result.length else toUpperMapToDest(iterator, sourceEnd)
                result.setSpan(span, destStart, destEnd, flags)
            }
            return result
        }


        @RequiresApi(29)
        private fun toUpperMapToDest(iterator: Edits.Iterator, sourceIndex: Int): Int {
            //
            // The code was taken from android.text.TextUtils
            //

            // Guaranteed to succeed if sourceIndex < source.length().
            iterator.findSourceIndex(sourceIndex)
            if (sourceIndex == iterator.sourceIndex()) {
                return iterator.destinationIndex()
            }
            // We handle the situation differently depending on if we are in the changed slice or an
            // unchanged one: In an unchanged slice, we can find the exact location the span
            // boundary was before and map there.
            //
            // But in a changed slice, we need to treat the whole destination slice as an atomic unit.
            // We adjust the span boundary to the end of that slice to reduce of the chance of adjacent
            // spans in the source overlapping in the result. (The choice for the end vs the beginning
            // is somewhat arbitrary, but was taken because we except to see slightly more spans only
            // affecting a base character compared to spans only affecting a combining character.)
            return if (iterator.hasChange()) {
                iterator.destinationIndex() + iterator.newLength()
            } else {
                // Move the index 1:1 along with this unchanged piece of text.
                iterator.destinationIndex() + (sourceIndex - iterator.sourceIndex())
            }
        }
    }

}