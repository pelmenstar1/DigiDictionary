package io.github.pelmenstar1.digiDict.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.prefs.AppPreferences

/**
 * Describes semantics of the settings. The main element of the descriptor is a group.
 * The group contains items or actions from the same area.
 * Groups are divided into two types:
 * - Item groups. Item has an icon, a name and a content. Also the item is connected to
 *   appropriate [AppPreferences.Entry] which the item represents.
 * - Action groups. Action has a name and a perform lambda. Action is represented by a button.
 */
class SettingsDescriptor(val groups: List<Group>) {
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

    class Action(
        val id: Int,
        @StringRes val nameRes: Int
    )

    sealed interface Group {
        @get:StringRes
        val titleRes: Int
    }

    class ItemGroup(@StringRes override val titleRes: Int, val items: List<Item>) : Group {
        // TODO: Implement as inline-class
        class Builder(@StringRes private val titleRes: Int) {
            private val items = ArrayList<Item>(4)

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

            fun build() = ItemGroup(titleRes, items)
        }
    }

    class ActionGroup(@StringRes override val titleRes: Int, val actions: List<Action>) : Group {
        // TODO: Implement as inline-class
        class Builder(@StringRes private val titleRes: Int) {
            private val actions = ArrayList<Action>(4)

            fun action(id: Int, @StringRes nameRes: Int) {
                actions.add(Action(id, nameRes))
            }

            fun build() = ActionGroup(titleRes, actions)
        }
    }

    // TODO: Implement as inline-class
    class Builder {
        private val groups = ArrayList<Group>(4)

        fun group(group: Group) {
            groups.add(group)
        }

        inline fun itemGroup(@StringRes titleRes: Int, items: ItemGroup.Builder.() -> Unit) {
            val builder = ItemGroup.Builder(titleRes).also(items)

            group(builder.build())
        }

        inline fun actionGroup(@StringRes titleRes: Int, actions: ActionGroup.Builder.() -> Unit) {
            val builder = ActionGroup.Builder(titleRes).also(actions)

            group(builder.build())
        }

        fun build(): SettingsDescriptor {
            return SettingsDescriptor(groups)
        }
    }
}

inline fun settingsDescriptor(groups: SettingsDescriptor.Builder.() -> Unit): SettingsDescriptor {
    val builder = SettingsDescriptor.Builder().also(groups)

    return builder.build()
}