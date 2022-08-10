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

class SettingsDescriptor(val blocks: List<Block>) {
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

    sealed interface Block {
        @get:StringRes
        val titleRes: Int
    }

    class ItemBlock(@StringRes override val titleRes: Int, val items: List<Item<*>>) : Block {
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
                contentBuilder: ItemContentBuilder.() -> ItemContent<T>,
            ) {
                item(
                    nameRes,
                    iconRes,
                    AppPreferences.Entries.preferenceEntry(),
                    ItemContentBuilder.contentBuilder(),
                )
            }

            fun build() = ItemBlock(titleRes, items)
        }
    }

    class ActionBlock(@StringRes override val titleRes: Int, val actions: List<Action>) : Block {
        class Builder(@StringRes private val titleRes: Int) {
            private val actions = ArrayList<Action>(4)

            fun action(
                @StringRes nameRes: Int,
                perform: (args: ActionArgs) -> Unit
            ) {
                actions.add(Action(nameRes, perform))
            }

            fun build() = ActionBlock(titleRes, actions)
        }
    }

    class Builder {
        private val blocks = ArrayList<Block>(4)

        fun block(block: Block) {
            blocks.add(block)
        }

        inline fun itemBlock(@StringRes titleRes: Int, blockBuilder: ItemBlock.Builder.() -> Unit) {
            val builder = ItemBlock.Builder(titleRes).also(blockBuilder)

            block(builder.build())
        }

        inline fun actionBlock(@StringRes titleRes: Int, blockBuilder: ActionBlock.Builder.() -> Unit) {
            val builder = ActionBlock.Builder(titleRes).also(blockBuilder)

            block(builder.build())
        }

        fun build(): SettingsDescriptor {
            return SettingsDescriptor(blocks)
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

inline fun settingsDescriptor(block: SettingsDescriptor.Builder.() -> Unit): SettingsDescriptor {
    val builder = SettingsDescriptor.Builder().also(block)

    return builder.build()
}