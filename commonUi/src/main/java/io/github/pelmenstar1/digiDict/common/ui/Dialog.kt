package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val NO_OP_DIALOG_ON_CLICK_LISTENER = DialogInterface.OnClickListener { _, _ -> }

fun showAlertDialog(
    context: Context,
    @StringRes messageId: Int,
    positiveButtonAction: DialogInterface.OnClickListener,
    negativeButtonAction: DialogInterface.OnClickListener = NO_OP_DIALOG_ON_CLICK_LISTENER
) {
    MaterialAlertDialogBuilder(context)
        .setMessage(messageId)
        .setPositiveButton(android.R.string.ok, positiveButtonAction)
        .setNegativeButton(android.R.string.cancel, negativeButtonAction)
        .show()
}

inline fun showAlertDialog(
    context: Context,
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit
) {
    showAlertDialog(context, messageId, { _, _ -> positionButtonAction() })
}

inline fun Fragment.showAlertDialog(
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit
) {
    showAlertDialog(requireContext(), messageId, positionButtonAction)
}