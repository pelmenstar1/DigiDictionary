package io.github.pelmenstar1.digiDict.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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

    class Item<T : Any>(
        @StringRes val nameRes: Int,
        @DrawableRes val iconRes: Int,
        val preferenceEntry: AppPreferences.Entry<T>,
        val content: ItemContent<T>
    )

    interface ActionArgs {
        fun <T> get(valueClass: Class<T>): T
    }

    class Action(
        @StringRes val nameRes: Int,
        val perform: (args: ActionArgs) -> Unit
    )

    sealed interface Group {
        @get:StringRes
        val titleRes: Int
    }

    class ItemGroup(@StringRes override val titleRes: Int, val items: List<Item<*>>) : Group {
        class Builder(@StringRes private val titleRes: Int) {
            private val items = ArrayList<Item<*>>(4)

            fun <T : Any> item(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int,
                preferenceEntry: AppPreferences.Entry<T>,
                content: ItemContent<T>,
            ) {
                items.add(Item(nameRes, iconRes, preferenceEntry, content))
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

            fun build() = ItemGroup(titleRes, items)
        }
    }

    class ActionGroup(@StringRes override val titleRes: Int, val actions: List<Action>) : Group {
        class Builder(@StringRes private val titleRes: Int) {
            private val actions = ArrayList<Action>(4)

            fun action(
                @StringRes nameRes: Int,
                perform: (args: ActionArgs) -> Unit
            ) {
                actions.add(Action(nameRes, perform))
            }

            fun build() = ActionGroup(titleRes, actions)
        }
    }

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

    companion object {
        inline fun <reified T> ActionArgs.get(): T {
            return get(T::class.java)
        }

        fun ActionArgs(vararg values: Any): ActionArgs {
            return object : ActionArgs {
                @Suppress("UNCHECKED_CAST")
                override fun <T> get(valueClass: Class<T>): T {
                    for (value in values) {
                        // isInstance is required here to allow subtypes of valueClass to be found.
                        if (valueClass.isInstance(value)) {
                            return value as T
                        }
                    }

                    throw IllegalArgumentException("No value found with class $valueClass")
                }
            }
        }
    }
}

inline fun settingsDescriptor(groups: SettingsDescriptor.Builder.() -> Unit): SettingsDescriptor {
    val builder = SettingsDescriptor.Builder().also(groups)

    return builder.build()
}