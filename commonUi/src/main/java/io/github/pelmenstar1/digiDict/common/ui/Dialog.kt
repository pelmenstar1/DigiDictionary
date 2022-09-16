package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val NO_OP_DIALOG_ON_CLICK_LISTENER = DialogInterface.OnClickListener { _, _ -> }

fun showAlertDialog(
    context: Context,
    message: CharSequence,
    positiveButtonAction: DialogInterface.OnClickListener,
    negativeButtonAction: DialogInterface.OnClickListener = NO_OP_DIALOG_ON_CLICK_LISTENER
) {
    MaterialAlertDialogBuilder(context)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, positiveButtonAction)
        .setNegativeButton(android.R.string.cancel, negativeButtonAction)
        .show()
}

fun showAlertDialog(
    context: Context,
    @StringRes messageId: Int,
    positiveButtonAction: DialogInterface.OnClickListener,
    negativeButtonAction: DialogInterface.OnClickListener = NO_OP_DIALOG_ON_CLICK_LISTENER
) {
    val message = context.resources.getText(messageId)

    showAlertDialog(context, message, positiveButtonAction, negativeButtonAction)
}

inline fun showAlertDialog(
    context: Context,
    message: String,
    crossinline positionButtonAction: () -> Unit,
    crossinline negativeButtonAction: () -> Unit = {}
) {
    showAlertDialog(context, message, { _, _ -> positionButtonAction() }, { _, _ -> negativeButtonAction() })
}

inline fun showAlertDialog(
    context: Context,
    message: String,
    crossinline positionButtonAction: () -> Unit
) {
    showAlertDialog(context, message, { _, _ -> positionButtonAction() })
}

inline fun showAlertDialog(
    context: Context,
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit,
    crossinline negativeButtonAction: () -> Unit
) {
    showAlertDialog(context, messageId, { _, _ -> positionButtonAction() }, { _, _ -> negativeButtonAction() })
}

inline fun showAlertDialog(
    context: Context,
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit
) {
    showAlertDialog(context, messageId, { _, _ -> positionButtonAction() })
}

inline fun Fragment.showAlertDialog(
    message: String,
    crossinline positionButtonAction: () -> Unit,
    crossinline negativeButtonAction: () -> Unit
) {
    showAlertDialog(requireContext(), message, positionButtonAction, negativeButtonAction)
}

inline fun Fragment.showAlertDialog(
    message: String,
    crossinline positionButtonAction: () -> Unit
) {
    showAlertDialog(requireContext(), message, positionButtonAction)
}

inline fun Fragment.showAlertDialog(
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit,
) {
    showAlertDialog(requireContext(), messageId, positionButtonAction)
}

inline fun Fragment.showAlertDialog(
    @StringRes messageId: Int,
    crossinline positionButtonAction: () -> Unit,
    crossinline negativeButtonAction: () -> Unit
) {
    showAlertDialog(requireContext(), messageId, positionButtonAction, negativeButtonAction)
}