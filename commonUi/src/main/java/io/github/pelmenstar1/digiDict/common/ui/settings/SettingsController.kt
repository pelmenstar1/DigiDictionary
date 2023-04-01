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

    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any, TDialog : DialogFragment> registerDialogForEntry(
        entry: AppPreferences.Entry<TValue, TEntries>,
        initializer: SettingsDialogInitializer<TValue, TDialog>
    ) {
        val descriptorDialog =
            descriptor.dialogs.find { it.entry == entry } as SettingsDescriptor.Dialog<TValue, TDialog, TEntries>?
                ?: throw IllegalStateException("The dialog associated with $entry should be registered in settings descriptor")

        dialogInfos.add(
            DialogInfo(
                entry,
                descriptorDialog.dialogClass,
                descriptorDialog.tag,
                descriptorDialog.createArgs,
                initializer
            )
        )
    }

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

    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers.put(id, handler)
    }

    fun <TValue : Any> bindContentItemClickAction(
        entry: AppPreferences.Entry<TValue, TEntries>,
        action: SettingsContentItemClickAction<TValue, TEntries>
    ) {
        contentItemClickActions[entry] = action
    }

    inline fun <TValue : Any> bindContentItemClickAction(
        entry: AppPreferences.Entry<TValue, TEntries>,
        action: SettingsContentItemClickActions.() -> SettingsContentItemClickAction<TValue, TEntries>
    ) {
        bindContentItemClickAction(entry, SettingsContentItemClickActions.action())
    }

    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> bindTextFormatter(
        entry: AppPreferences.Entry<TValue, TEntries>,
        formatter: StringFormatter<TValue>
    ) {
        textFormatters[entry] = formatter as StringFormatter<Any>
    }

    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> onValueChanged(entry: AppPreferences.Entry<TValue, TEntries>, newValue: TValue) {
        (onValueChangedHandler as ((AppPreferences.Entry<TValue, TEntries>, TValue) -> Unit)?)?.invoke(entry, newValue)
    }

    fun performAction(id: Int) {
        val handler = actionHandlers.get(id)

        handler?.invoke()
    }

    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> performContentItemClickListener(entry: AppPreferences.Entry<TValue, TEntries>) {
        val handler = contentItemClickActions[entry] as SettingsContentItemClickAction<TValue, TEntries>?

        handler?.perform(entry, this)
    }

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

    fun navigate(directions: NavDirections) {
        navController?.navigate(directions)
    }
}