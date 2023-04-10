package io.github.pelmenstar1.digiDict.common.ui.settings

import android.os.Bundle
import android.util.SparseArray
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.common.IntegerStringFormatter
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

class SettingsController<TEntries : AppPreferences.Entries>(
    private val descriptor: SettingsDescriptor<TEntries>,
    private val container: ViewGroup
) {
    private class DialogInfo<TEntries : AppPreferences.Entries, TValue : Any, TDialog : DialogFragment>(
        val entry: AppPreferences.Entry<TValue, TEntries>,
        val dialogClass: Class<TDialog>,
        val tag: String,
        val createArgs: ((TValue) -> Bundle)? = null,
        val initializer: SettingsDialogInitializer<TValue, TDialog>
    )

    private val actionHandlers = SparseArray<() -> Unit>()
    private val contentItemClickActions =
        ArrayMap<AppPreferences.Entry<*, TEntries>, SettingsContentItemClickAction<*, TEntries>>()
    private val textFormatters = ArrayMap<AppPreferences.Entry<*, TEntries>, StringFormatter<Any>>()
    private val dialogInfos = ArrayList<DialogInfo<TEntries, *, *>>()

    private var currentSnapshot: AppPreferences.Snapshot<TEntries>? = null

    private var isDialogsInitialized = false

    var navController: NavController? = null
    var onValueChangedHandler: ((AppPreferences.Entry<Any, TEntries>, Any) -> Unit)? = null
    var childFragmentManager: FragmentManager? = null

    private fun requireChildFragmentManager(): FragmentManager {
        return childFragmentManager ?: throw IllegalStateException("childFragmentManager should be non-null")
    }

    private fun requireSnapshot(): AppPreferences.Snapshot<TEntries> {
        return currentSnapshot ?: throw IllegalStateException("No snapshot has been applied")
    }

    private fun generateTag(dialogClass: Class<out DialogFragment>, entry: AppPreferences.Entry<*, TEntries>): String {
        return "${dialogClass.simpleName}_${entry.name}"
    }

    /**
     * Completely registers the dialog by specifying [initializer].
     *
     * The initializer will be called when the dialog is created and
     * when the host fragment is recreated and the dialog is on screen. If the host fragment is recreated, the initializers will be
     * called once when first snapshot is applied.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any, TDialog : DialogFragment> registerDialogForEntry(
        entry: AppPreferences.Entry<TValue, TEntries>,
        initializer: SettingsDialogInitializer<TValue, TDialog>
    ) {
        val descriptorDialog =
            descriptor.dialogs.find { it.entry == entry } as SettingsDescriptor.Dialog<TValue, TDialog, TEntries>?
                ?: throw IllegalStateException("The dialog associated with $entry should be registered in settings descriptor")

        val dialogClass = descriptorDialog.dialogClass

        dialogInfos.add(
            DialogInfo(
                entry,
                dialogClass,
                descriptorDialog.tag ?: generateTag(dialogClass, entry),
                descriptorDialog.createArgs,
                initializer
            )
        )
    }

    /**
     * Shows the dialog associated with specified [entry]. Expects that any snapshot was applied and [childFragmentManager] is not null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> showDialogForEntry(entry: AppPreferences.Entry<TValue, TEntries>) {
        val fm = requireChildFragmentManager()
        val info = dialogInfos.find { it.entry == entry } as DialogInfo<TEntries, TValue, DialogFragment>?
            ?: throw IllegalStateException("No dialog registered for specified entry")

        val entryValue = requireSnapshot()[entry]

        val fragment = info.dialogClass.newInstance() as DialogFragment
        fragment.arguments = info.createArgs?.invoke(entryValue)
        info.initializer.init(fragment, entryValue)

        fragment.show(fm, info.tag)
    }

    /**
     * Applies [snapshot]. This will update UI as well.
     */
    fun applySnapshot(snapshot: AppPreferences.Snapshot<TEntries>) {
        currentSnapshot = snapshot

        SettingsInflater.applySnapshot(this, snapshot, container)

        if (!isDialogsInitialized) {
            isDialogsInitialized = true

            initDialogsIfShown()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initDialogsIfShown() {
        val fm = requireChildFragmentManager()
        val snapshot = requireSnapshot()

        for (descriptorDialog in descriptor.dialogs) {
            val tag = descriptorDialog.tag
            val dialog = fm.findFragmentByTag(tag) as DialogFragment?

            if (dialog != null) {
                val info = dialogInfos.find { it.tag == tag } as DialogInfo<TEntries, Any, DialogFragment>?
                    ?: throw IllegalStateException("No dialog registered with tag: $tag")

                val entryValue = snapshot[info.entry]
                info.initializer.init(dialog, entryValue)
            }
        }
    }

    /**
     * Sets a handler that will be invoked when user clicks on item with specified [id].
     */
    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers.put(id, handler)
    }

    /**
     * Sets a [SettingsContentItemClickAction] for item associated with given [entry].
     */
    fun <TValue : Any> bindContentItemClickAction(
        entry: AppPreferences.Entry<TValue, TEntries>,
        action: SettingsContentItemClickAction<TValue, TEntries>
    ) {
        contentItemClickActions[entry] = action
    }

    /**
     * Sets a [SettingsContentItemClickAction] for item associated with given [entry].
     */
    inline fun <TValue : Any> bindContentItemClickAction(
        entry: AppPreferences.Entry<TValue, TEntries>,
        action: SettingsContentItemClickActions.() -> SettingsContentItemClickAction<TValue, TEntries>
    ) {
        bindContentItemClickAction(entry, SettingsContentItemClickActions.action())
    }

    /**
     * Sets a custom text formatter for item, with text content and associated with given [entry].
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> bindTextFormatter(
        entry: AppPreferences.Entry<TValue, TEntries>,
        formatter: StringFormatter<TValue>
    ) {
        textFormatters[entry] = formatter as StringFormatter<Any>
    }

    /**
     * The method should be called when preferences value is changed by the UI.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> onValueChanged(entry: AppPreferences.Entry<TValue, TEntries>, newValue: TValue) {
        (onValueChangedHandler as ((AppPreferences.Entry<TValue, TEntries>, TValue) -> Unit)?)?.invoke(entry, newValue)
    }

    /**
     * Performs an action, if it is specified, for the action item with given [id].
     */
    fun performAction(id: Int) {
        val handler = actionHandlers.get(id)

        handler?.invoke()
    }

    /**
     * Performs an action, if it is specified, for the content item associated with given [entry]
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> performContentItemClickListener(entry: AppPreferences.Entry<TValue, TEntries>) {
        val handler = contentItemClickActions[entry] as SettingsContentItemClickAction<TValue, TEntries>?

        handler?.perform(entry, this)
    }

    /**
     * Returns a text formatter for item associated with specified [entry]. If no text formatter is set, the method will try
     * to lookup for known types (currently only [Int]), and return predefined text formatter. If [entry]'s value class isn't "known",
     * the method returns null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> getTextFormatter(entry: AppPreferences.Entry<TValue, TEntries>): StringFormatter<TValue>? {
        val formatter = textFormatters[entry] as StringFormatter<TValue>?

        if (formatter == null) {
            val valueClass = entry.valueClass

            // Integer is a known type and simple to format
            if (valueClass == Int::class.javaObjectType) {
                return IntegerStringFormatter as StringFormatter<TValue>
            }
        }

        return formatter
    }

    /**
     * Navigates to specified [directions]. No-op if [navController] is `null`.
     */
    fun navigate(directions: NavDirections) {
        navController?.navigate(directions)
    }
}