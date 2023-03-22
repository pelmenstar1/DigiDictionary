package io.github.pelmenstar1.digiDict.ui.paging

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.EventInfo
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputedValues

/**
 * Describes the data holder of page item for use in the application.
 */
sealed interface PageItem {
    /**
     * Determines whether this page item represents the same item as [other] page item.
     * It should check the whole contents rather some kind of id.
     *
     * The expected behaviour is alike to [DiffUtil.ItemCallback.areItemsTheSame]
     */
    fun isTheSameTo(other: PageItem): Boolean

    /**
     * Determines whether this page item holds the same content as [other] page item.
     *
     *  The expected behaviour is alike to [DiffUtil.ItemCallback.areContentsTheSame]
     */
    fun isSameContentWith(other: PageItem): Boolean

    /**
     * Represents a record item that holds the [ConciseRecordWithBadges] and some other flags.
     * [precomputedValues] is used only as supplementary data and doesn't affect [equals], [hashCode], [toString]
     *
     * @param isBeforeDateMarker marks whether this record is before [DateMarker] in a collection or sequence of any kind
     */
    class Record(
        val record: ConciseRecordWithBadges,
        val isBeforeDateMarker: Boolean,
        val precomputedValues: RecordTextPrecomputedValues?
    ) : PageItem {
        override fun isTheSameTo(other: PageItem): Boolean {
            return other is Record && other.record.id == record.id
        }

        override fun isSameContentWith(other: PageItem): Boolean {
            return other is Record && isSameContentWith(other)
        }

        private fun isSameContentWith(other: Record): Boolean {
            return other.record == record && other.isBeforeDateMarker == isBeforeDateMarker
        }

        override fun equals(other: Any?) = equalsPattern(other, ::isSameContentWith)

        override fun hashCode(): Int {
            var result = record.hashCode()
            result = result * 31 + (if (isBeforeDateMarker) 1 else 0)

            return result
        }

        override fun toString(): String {
            return "PageItem.Record(record=$record, isBeforeDateMarker=$isBeforeDateMarker)"
        }
    }

    /**
     * Represents a date marker that shows a formatted date.
     *
     * @param epochDay epoch day that describes the date. It's expected to be local epoch day.
     */
    class DateMarker(val epochDay: Long) : PageItem {
        override fun isTheSameTo(other: PageItem) = isSameContentWith(other)

        override fun isSameContentWith(other: PageItem): Boolean {
            return other is DateMarker && epochDay == other.epochDay
        }

        override fun equals(other: Any?) = equalsPattern(other) { epochDay == it.epochDay }

        override fun hashCode(): Int {
            return (epochDay xor (epochDay ushr 32)).toInt()
        }

        override fun toString(): String {
            return "PageItem.DateMarker(epochDay=$epochDay)"
        }
    }

    /**
     * Represents an event marker that shows whether the event is started or ended.
     *
     * @param isStarted determines whether this is the marker that shows that event is started
     */
    class EventMarker(val isStarted: Boolean, val event: EventInfo) : PageItem {
        override fun isTheSameTo(other: PageItem): Boolean {
            return other is EventMarker && event.id == other.event.id
        }

        override fun isSameContentWith(other: PageItem): Boolean {
            return other is EventMarker && isSameContentWith(other)
        }

        private fun isSameContentWith(other: EventMarker): Boolean {
            return isStarted == other.isStarted && event == other.event
        }

        override fun equals(other: Any?) = equalsPattern(other, ::isSameContentWith)

        override fun hashCode(): Int {
            var result = event.hashCode()
            result = result * 31 + if (isStarted) 1 else 0

            return result
        }

        override fun toString(): String {
            return "PageItem.EventMarker(isStarted=$isStarted, event=$event)"
        }
    }
}

/**
 * An implementation of [DiffUtil.ItemCallback] that compares [PageItem]'s
 */
object PageItemDiffCallback : DiffUtil.ItemCallback<PageItem>() {
    override fun areItemsTheSame(oldItem: PageItem, newItem: PageItem): Boolean {
        return oldItem.isTheSameTo(newItem)
    }

    override fun areContentsTheSame(oldItem: PageItem, newItem: PageItem): Boolean {
        return oldItem.isSameContentWith(newItem)
    }
}