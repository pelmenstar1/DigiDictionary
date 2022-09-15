package io.github.pelmenstar1.digiDict.ui.exportConfig

import io.github.pelmenstar1.digiDict.backup.BackupFormat

data class ExportFormatEntry(val enumValue: BackupFormat, val name: String, val descriptionId: Int)