package io.github.pelmenstar1.digiDict.common.ui.settings

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

/**
 * Describes semantics of the settings. The main element of the descriptor is a group.
 * The group contains items from the same area.
 */
class SettingsDescriptor<TEntries : AppPreferences.Entries>(
    val groups: List<ItemGroup>,
    val dialogs: List<Dialog<*, *, TEntries>>
) {
    object ItemContentBuilder {
        fun switch(): ItemContent<Boolean> = SwitchItemContent

        fun <T : Any> text(): ItemContent<T> = TextContent()
    }

    /**
     * Marker interface which indicates that the class represents a particular content info for class [T]
     */
    sealed interface ItemContent<out T : Any>

    object SwitchItemContent : ItemContent<Boolean>

    class TextContent<T : Any> : ItemContent<T>

    sealed interface Item

    class ContentItem<TValue : Any, TEntries : AppPreferences.Entries>(
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
        value class ItemListBuilder<TEntries : AppPreferences.Entries>(private val items: MutableList<Item>) {
            /**
             * Adds content item to a group.
             *
             * @param nameRes a string resource id of name of this item
             * @param iconRes a drawable resource id of icon of this item
             * @param preferenceEntry an entry, in preferences, that this item represents
             * @param content content of this item
             */
            fun <TValue : Any> item(
                @StringRes nameRes: Int,
                @DrawableRes iconRes: Int? = null,
                preferenceEntry: AppPreferences.Entry<TValue, TEntries>,
                clickable: Boolean = false,
                content: ItemContent<TValue>,
            ) {
                items.add(ContentItem(nameRes, iconRes, preferenceEntry, clickable, content))
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
    value class GroupListBuilder<TEntries : AppPreferences.Entries>(private val groups: MutableList<ItemGroup>) {
        fun group(group: ItemGroup) {
            groups.add(group)
        }

        inline fun group(@StringRes titleRes: Int, itemsBlock: ItemGroup.ItemListBuilder<TEntries>.() -> Unit) {
            val items = ArrayList<Item>()
            ItemGroup.ItemListBuilder<TEntries>(items).also(itemsBlock)

            group(ItemGroup(titleRes, items))
        }
    }

    data class Dialog<TValue : Any, TDialog : DialogFragment, TEntries : AppPreferences.Entries>(
        val dialogClass: Class<TDialog>,
        val tag: String?,
        val entry: AppPreferences.Entry<TValue, TEntries>,
        val createArgs: ((selectedValue: TValue) -> Bundle)? = null
    )

    @JvmInline
    value class DialogListBuilder<TEntries : AppPreferences.Entries>(private val dialogs: MutableList<Dialog<*, *, TEntries>>) {
        /**
         * A shortcut for [dialog] method with explicitly specified class of the dialog.
         */
        inline fun <TValue : Any, reified TDialog : DialogFragment> dialog(
            tag: String? = null,
            entry: AppPreferences.Entry<TValue, TEntries>,
            noinline createArgs: ((selectedValue: TValue) -> Bundle)? = null
        ) = dialog(TDialog::class.java, tag, entry, createArgs)

        /**
         * Adds information about dialog associated with specified [entry].
         * If the dialog is specified to [entry], the item, associated with [entry], will show the dialog on click.
         *
         * @param dialogClass class of the dialog
         * @param tag a tag of the dialog. It should be unique among all fragments in child fragment manager of host fragment.
         * If null, the tag will be generated via concatenating dialog class name and entry name
         * @param entry a preference entry the dialog is associated with
         * @param createArgs a lambda to create arguments for dialog fragment.
         * Can be null to set `null` to arguments of the dialog fragment
         */
        fun <TValue : Any> dialog(
            dialogClass: Class<out DialogFragment>,
            tag: String? = null,
            entry: AppPreferences.Entry<TValue, TEntries>,
            createArgs: ((selectedValue: TValue) -> Bundle)? = null
        ) {
            dialogs.add(Dialog(dialogClass, tag, entry, createArgs))
        }
    }
}

class SettingsDescriptorBuilder<TEntries : AppPreferences.Entries> {
    private val groupsList = ArrayList<SettingsDescriptor.ItemGroup>()
    private val dialogsList = ArrayList<SettingsDescriptor.Dialog<*, *, TEntries>>()

    fun groups(block: SettingsDescriptor.GroupListBuilder<TEntries>.() -> Unit) {
        block(SettingsDescriptor.GroupListBuilder(groupsList))
    }

    fun dialogs(block: SettingsDescriptor.DialogListBuilder<TEntries>.() -> Unit) {
        block(SettingsDescriptor.DialogListBuilder(dialogsList))
    }

    fun create(): SettingsDescriptor<TEntries> {
        return SettingsDescriptor(groupsList, dialogsList)
    }
}

inline fun <TEntries : AppPreferences.Entries> settingsDescriptor(
    block: SettingsDescriptorBuilder<TEntries>.() -> Unit
): SettingsDescriptor<TEntries> {
    return SettingsDescriptorBuilder<TEntries>().also(block).create()
}