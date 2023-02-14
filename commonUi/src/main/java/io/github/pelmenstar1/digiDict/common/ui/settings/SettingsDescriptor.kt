package io.github.pelmenstar1.digiDict.common.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

/**
 * Describes semantics of the settings. The main element of the descriptor is a group.
 * The group contains items from the same area.
 */
class SettingsDescriptor(val groups: List<ItemGroup>) {
    object ItemContentBuilder {
        fun switch(): ItemContent<Boolean> = SwitchItemContent

        fun rangeSpinner(
            start: Int,
            endInclusive: Int,
            step: Int = 1,
        ): ItemContent<Int> {
            return RangeSpinnerItemContent(start, endInclusive, step)
        }

        fun <T : Any> text(): ItemContent<T> = TextContent()
    }

    /**
     * Marker interface which indicates that the class represents a particular content info for class [T]
     */
    sealed interface ItemContent<out T : Any>

    object SwitchItemContent : ItemContent<Boolean>

    class RangeSpinnerItemContent(
        val start: Int,
        val endInclusive: Int,
        val step: Int,
    ) : ItemContent<Int>

    class TextContent<T : Any> : ItemContent<T>

    sealed interface Item

    class ContentItem<TValue : Any, TEntries : AppPreferences.Entries>(
        val id: Int,
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int?,
        val preferenceEntry: AppPreferences.Entry<TValue, TEntries>,
        val clickable: Boolean,
        val content: ItemContent<TValue>
    ) : Item

    class LinkItem(
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int?,
        val directions: NavDirections
    ) : Item

    class ActionItem(
        val id: Int,
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int?
    ) : Item

    class ItemGroup(@StringRes val titleRes: Int, val items: List<Item>) {
        @JvmInline
        value class ItemListBuilder(private val items: MutableList<Item>) {
            /**
             * Adds content item to a group.
             *
             * @param id id of this item. When item has no id, the id should be [SettingsDescriptor.ITEM_ID_UNSPECIFIED]
             * The id should be unique among all content items.
             * @param nameRes a string resource id of name of this item
             * @param iconRes a drawable resource id of icon of this item
             * @param preferenceEntry an entry, in preferences, that this item represents
             * @param content content of this item
             */
            fun <TValue : Any, TEntries : AppPreferences.Entries> item(
                id: Int = ITEM_ID_UNSPECIFIED,
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int? = null,
                preferenceEntry: AppPreferences.Entry<TValue, TEntries>,
                clickable: Boolean = false,
                content: ItemContent<TValue>,
            ) {
                items.add(ContentItem(id, nameRes, iconRes, preferenceEntry, clickable, content))
            }

            /**
             * Adds link item to a group. Link item is an item that navigates to another fragment on click.
             *
             * @param nameRes a string resource id of name of this item
             * @param iconRes a drawable resource id of icon of this item. It can be null in case the item has no icon
             * @param directions contains a description of fragment to navigate on click
             */
            fun linkItem(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int? = null,
                directions: NavDirections
            ) {
                items.add(LinkItem(nameRes, iconRes, directions))
            }

            /**
             * Adds action item to a group. Action item is an item that executes some action on click.
             *
             * @param id id of the action. It should be unique among all action items
             * @param nameRes a string resource of name of this item
             * @param iconRes a drawable resource of icon of this item. It can be null in case the item has no icon.
             */
            fun actionItem(
                id: Int,
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int? = null
            ) {
                items.add(ActionItem(id, nameRes, iconRes))
            }
        }
    }

    @JvmInline
    value class GroupListBuilder(private val groups: MutableList<ItemGroup>) {
        fun group(group: ItemGroup) {
            groups.add(group)
        }

        inline fun group(@StringRes titleRes: Int, itemsBlock: ItemGroup.ItemListBuilder.() -> Unit) {
            val items = ArrayList<Item>()
            ItemGroup.ItemListBuilder(items).also(itemsBlock)

            group(ItemGroup(titleRes, items))
        }
    }

    companion object {
        const val ITEM_ID_UNSPECIFIED = -1
    }
}

inline fun settingsDescriptor(groupsBuilder: SettingsDescriptor.GroupListBuilder.() -> Unit): SettingsDescriptor {
    val groups = ArrayList<SettingsDescriptor.ItemGroup>()
    SettingsDescriptor.GroupListBuilder(groups).also(groupsBuilder)

    return SettingsDescriptor(groups)
}