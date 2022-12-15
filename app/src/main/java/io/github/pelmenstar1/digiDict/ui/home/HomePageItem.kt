package io.github.pelmenstar1.digiDict.ui.home

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

sealed interface HomePageItem {
    fun isTheSameTo(other: HomePageItem): Boolean
    fun isSameContentWith(other: HomePageItem): Boolean

    class Record(val value: ConciseRecordWithBadges, val isBeforeDateMarker: Boolean) : HomePageItem {
        override fun isTheSameTo(other: HomePageItem): Boolean {
            return if (other is Record) {
                other.value.id == value.id && other.isBeforeDateMarker == isBeforeDateMarker
            } else {
                false
            }
        }

        override fun isSameContentWith(other: HomePageItem): Boolean {
            return if (other is Record) {
                other.value == value && other.isBeforeDateMarker == isBeforeDateMarker
            } else {
                false
            }
        }
    }

    class DateMarker(val epochDay: Long) : HomePageItem {
        override fun isTheSameTo(other: HomePageItem) = isSameContentWith(other)

        override fun isSameContentWith(other: HomePageItem): Boolean {
            return if (other is DateMarker) {
                epochDay == other.epochDay
            } else {
                false
            }
        }
    }
}

object HomePageItemDiffCallback : DiffUtil.ItemCallback<HomePageItem>() {
    override fun areItemsTheSame(oldItem: HomePageItem, newItem: HomePageItem): Boolean {
        return oldItem.isTheSameTo(newItem)
    }

    override fun areContentsTheSame(oldItem: HomePageItem, newItem: HomePageItem): Boolean {
        return oldItem.isSameContentWith(newItem)
    }
}