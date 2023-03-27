package io.github.pelmenstar1.digiDict.common.android

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class MaterialDialogFragment : DialogFragment() {
    private var dialogView: View? = null

    override fun getView() = dialogView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showsDialog = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme).apply {
            dialogView = createDialogView(layoutInflater, savedInstanceState)

            setView(dialogView)
        }.create()
    }

    protected abstract fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View
}