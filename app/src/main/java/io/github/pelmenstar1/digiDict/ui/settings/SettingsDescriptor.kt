package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatSpinner
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.pelmenstar1.digiDict.common.createNumberRangeList
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import kotlin.math.min

/**
 * Describes semantics of the settings. The main element of the descriptor is a group.
 * The group contains items or actions from the same area.
 * Groups are divided into two types:
 * - Item groups. Item has an icon, a name and a content. Also the item is connected to
 *   appropriate [AppPreferences.Entry] which the item represents. [SettingsDescriptor.ItemContent] represents the means to
 *   create a widget and to set the current value of the [AppPreferences.Entry] to the created widget. The widget must be
 *   interactive, a user should have a way to change the value and when the value is changed, it's reported through [SettingsDescriptor.ItemContentInterface].
 *   Also the widget can use [View.getTag]/[View.setTag], it won't be overwritten.
 * - Action groups. Action has a name and a perform lambda. Action is represented by a button.
 */
class SettingsDescriptor(val groups: List<Group>) {
    interface ItemContentInterface<in T : Any> {
        fun onValueChanged(value: T)
    }

    object ItemContentBuilder {
        fun switch(): ItemContent<Boolean> {
            return SwitchItemContent()
        }

        fun rangeSpinner(
            start: Int,
            endInclusive: Int,
            step: Int = 1,
        ): ItemContent<Int> {
            return RangeSpinnerContent(start, endInclusive, step)
        }
    }

    interface ItemContent<T : Any> {
        fun createView(
            context: Context,
            itemInterface: ItemContentInterface<T>,
        ): View

        fun setValue(view: View, value: T)
    }

    private class SwitchItemContent : ItemContent<Boolean> {
        override fun createView(
            context: Context,
            itemInterface: ItemContentInterface<Boolean>
        ): View {
            return SwitchMaterial(context).also {
                it.tag = itemInterface

                it.setOnCheckedChangeListener(onCheckedChangedListener)
            }
        }

        override fun setValue(view: View, value: Boolean) {
            (view as SwitchMaterial).isChecked = value
        }

        companion object {
            @Suppress("UNCHECKED_CAST")
            private val onCheckedChangedListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
                (view.tag as ItemContentInterface<Boolean>).also {
                    it.onValueChanged(isChecked)
                }
            }
        }
    }

    private class RangeSpinnerContent(
        private val start: Int,
        private val endInclusive: Int,
        private val step: Int,
    ) : ItemContent<Int> {
        private class Tag(
            @JvmField val start: Int,
            @JvmField val endInclusive: Int,
            @JvmField val step: Int,
            @JvmField val itemInterface: ItemContentInterface<Int>
        )

        private val adapterItems = createNumberRangeList(start, endInclusive, step)

        override fun createView(context: Context, itemInterface: ItemContentInterface<Int>): View {
            return AppCompatSpinner(context).also {
                it.tag = Tag(start, endInclusive, step, itemInterface)

                it.adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    adapterItems
                )

                it.onItemSelectedListener = onItemSelectedListener
            }
        }

        override fun setValue(view: View, value: Int) {
            val constrainedValue = value.coerceIn(start, endInclusive)
            val position = (constrainedValue - start) / step

            (view as AppCompatSpinner).setSelection(position)
        }

        companion object {
            private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                @Suppress("UNCHECKED_CAST")
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    (parent.tag as Tag).also {
                        val value = min(it.endInclusive, it.start + position * it.step)

                        it.itemInterface.onValueChanged(value)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

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