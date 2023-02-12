package io.github.pelmenstar1.digiDict.ui.paging

import androidx.recyclerview.widget.DiffUtil
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
     *
     * @param isBeforeDateMarker marks whether this record is before [DateMarker] in a collection or sequence of any kind
     */
    class Record(
        val record: ConciseRecordWithBadges,
        val isBeforeDateMarker: Boolean,
        val precomputedValues: RecordTextPrecomputedValues?
    ) : PageItem {
        override fun isTheSameTo(other: PageItem): Boolean {
            return if (other is Record) {
                other.record.id == record.id
            } else {
                false
            }
        }

        override fun isSameContentWith(other: PageItem): Boolean {
            return if (other is Record) {
                other.record == record && other.isBeforeDateMarker == isBeforeDateMarker
            } else {
                false
            }
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
            return if (other is DateMarker) {
                epochDay == other.epochDay
            } else {
                false
            }
        }
    }

    /**
     * Represents an event marker that shows whether the event is started or ended.
     *
     * @param isStarted determines whether this is the marker that shows that event is started
     */
    class EventMarker(val isStarted: Boolean, val event: EventInfo) : PageItem {
        override fun isTheSameTo(other: PageItem): Boolean {
            return if (other is EventMarker) {
                event.id == other.event.id
            } else {
                false
            }
        }

        override fun isSameContentWith(other: PageItem): Boolean {
            return if (other is EventMarker) {
                isStarted == other.isStarted && event == other.event
            } else {
                false
            }
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