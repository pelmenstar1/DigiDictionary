package io.github.pelmenstar1.digiDict.backup

enum class BackupFormat(@JvmField val extension: String, val shortName: String, val latestVersion: Int) {
    DDDB(extension = "dddb", shortName = "DDDB", latestVersion = 1),
    JSON(extension = "json", shortName = "JSON", latestVersion = 1);

    companion object {
        fun fromExtensionOrNull(value: String) = when (value) {
            "dddb" -> DDDB
            "json" -> JSON
            else -> null
        }
    }
}