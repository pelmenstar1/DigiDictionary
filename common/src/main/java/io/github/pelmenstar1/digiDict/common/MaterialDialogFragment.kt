package io.github.pelmenstar1.digiDict.common

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class MaterialDialogFragment : DialogFragment() {
    private var dialogView: View? = null

    override fun getView() = dialogView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme).apply {
            dialogView = onCreateView(layoutInflater, null, savedInstanceState)

            setView(dialogView)
        }.create()
    }
}