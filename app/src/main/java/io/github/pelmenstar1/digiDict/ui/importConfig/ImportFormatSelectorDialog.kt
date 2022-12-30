package io.github.pelmenstar1.digiDict.ui.importConfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.databinding.DialogImportFormatSelectorBinding

class ImportFormatSelectorDialog : MaterialDialogFragment() {
    var onFormatSelected: ((BackupFormat) -> Unit)? = null

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val binding = DialogImportFormatSelectorBinding.inflate(layoutInflater, null, false)

        binding.importFormatSelectorRecyclerView.apply {
            adapter = ImportFormatSelectorDialogAdapter(
                onItemClickListener = {
                    onFormatSelected?.invoke(it)
                    dismiss()
                }
            ).also {
                it.submitItems(BackupFormat.values())
            }

            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        return binding.root
    }
}