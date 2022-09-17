package io.github.pelmenstar1.digiDict.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.prefs.AppPreferences

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

    sealed interface Item

    class ContentItem<T : Any>(
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int,
        val preferenceEntry: AppPreferences.Entry<T>,
        val content: ItemContent<T>
    ) : Item

    class LinkItem(
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int,
        val directions: NavDirections
    ) : Item

    class ActionItem(
        val id: Int,
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int
    ) : Item

    class ItemGroup(@StringRes val titleRes: Int, val items: List<Item>) {
        @JvmInline
        value class ItemListBuilder(private val items: MutableList<Item>) {
            fun <T : Any> item(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int,
                preferenceEntry: AppPreferences.Entry<T>,
                content: ItemContent<T>,
            ) {
                items.add(ContentItem(nameRes, iconRes, preferenceEntry, content))
            }

            inline fun <T : Any> item(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int,
                preferenceEntry: AppPreferences.Entries.() -> AppPreferences.Entry<T>,
                content: ItemContentBuilder.() -> ItemContent<T>,
            ) {
                item(
                    nameRes,
                    iconRes,
                    AppPreferences.Entries.preferenceEntry(),
                    ItemContentBuilder.content(),
                )
            }

            fun linkItem(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int = -1,
                directions: NavDirections
            ) {
                items.add(LinkItem(nameRes, iconRes, directions))
            }

            fun actionItem(
                id: Int,
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int = -1
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
}

inline fun settingsDescriptor(groupsBuilder: SettingsDescriptor.GroupListBuilder.() -> Unit): SettingsDescriptor {
    val groups = ArrayList<SettingsDescriptor.ItemGroup>()
    SettingsDescriptor.GroupListBuilder(groups).also(groupsBuilder)

    return SettingsDescriptor(groups)
}