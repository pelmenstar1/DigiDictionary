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
        val init: (TDialog) -> Unit
    )

    private val actionHandlers = SparseArray<() -> Unit>()
    private val textFormatters = ArrayMap<AppPreferences.Entry<*, TEntries>, StringFormatter<*>>()
    private val dialogInfos = ArrayList<DialogInfo<TEntries, *, *>>()

    private var currentSnapshot: AppPreferences.Snapshot<TEntries>? = null

    var navController: NavController? = null
    var onValueChangedHandler: ((AppPreferences.Entry<Any, TEntries>, Any) -> Unit)? = null
    var childFragmentManager: FragmentManager? = null

    private fun requireChildFragmentManager(): FragmentManager {
        return childFragmentManager ?: throw IllegalStateException("childFragmentManager should be non-null")
    }

    private fun generateTag(dialogClass: Class<out DialogFragment>, entry: AppPreferences.Entry<*, TEntries>): String {
        return "${dialogClass.simpleName}_${entry.name}"
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any, TDialog : DialogFragment> findDialogInfoByEntry(
        entry: AppPreferences.Entry<TValue, TEntries>
    ): DialogInfo<TEntries, TValue, TDialog>? {
        return dialogInfos.find { it.entry == entry } as DialogInfo<TEntries, TValue, TDialog>?
    }

    /**
     * Completely registers the dialog by specifying [initializer].
     *
     * The initializer will be called when the dialog is created and
     * when the host fragment is recreated and the dialog is on screen.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any, TDialog : DialogFragment> registerDialogForEntry(
        entry: AppPreferences.Entry<TValue, TEntries>,
        initializer: (TDialog) -> Unit
    ) {
        val descriptorDialog =
            descriptor.dialogs.find { it.entry == entry } as SettingsDescriptor.Dialog<TValue, TDialog, TEntries>?
                ?: throw IllegalStateException("The dialog associated with ${entry.name} should be registered in settings descriptor")

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

    private fun <TValue : Any, TDialog : DialogFragment> showDialogForEntry(info: DialogInfo<TEntries, TValue, TDialog>) {
        val fm = requireChildFragmentManager()
        val snapshot = currentSnapshot ?: throw IllegalStateException("No snapshot has been applied")
        val entryValue = snapshot[info.entry]

        val fragment = info.dialogClass.newInstance()
        fragment.arguments = info.createArgs?.invoke(entryValue)
        info.init(fragment)

        fragment.show(fm, info.tag)
    }

    /**
     * Applies [snapshot]. This will update UI as well.
     */
    fun applySnapshot(snapshot: AppPreferences.Snapshot<TEntries>) {
        currentSnapshot = snapshot

        SettingsInflater.applySnapshot(this, snapshot, container)
    }

    /**
     * Initializes all previously registered dialogs (via [registerDialogForEntry]) if they are on the screen.
     */
    @Suppress("UNCHECKED_CAST")
    fun initDialogsIfShown() {
        val fm = requireChildFragmentManager()

        for (info in dialogInfos) {
            info as DialogInfo<TEntries, *, DialogFragment>

            (fm.findFragmentByTag(info.tag) as DialogFragment?)?.also(info.init)
        }
    }

    /**
     * Sets a handler that will be invoked when user clicks on item with specified [id].
     */
    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers[id] = handler
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
        actionHandlers[id]?.invoke()
    }

    /**
     * Performs an action, if it is specified, for the content item associated with given [entry]
     */
    fun <TValue : Any> performContentItemClickListener(entry: AppPreferences.Entry<TValue, TEntries>) {
        findDialogInfoByEntry<TValue, DialogFragment>(entry)?.also { showDialogForEntry(it) }
    }

    /**
     * Returns a text formatter for item associated with specified [entry]. If no text formatter is set, the method will try
     * to lookup for known types (currently only [Int]), and return predefined text formatter. If [entry]'s value class isn't "known",
     * the method returns null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> getTextFormatter(entry: AppPreferences.Entry<TValue, TEntries>): StringFormatter<TValue>? {
        var formatter = textFormatters[entry]

        if (formatter == null) {
            val valueClass = entry.valueClass

            if (valueClass == Int::class.javaObjectType || valueClass == Int::class.java) {
                formatter = IntegerStringFormatter
            }
        }

        return formatter as StringFormatter<TValue>?
    }

    /**
     * Navigates to specified [directions]. No-op if [navController] is `null`.
     */
    fun navigate(directions: NavDirections) {
        navController?.navigate(directions)
    }
}