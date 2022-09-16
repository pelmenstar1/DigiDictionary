package io.github.pelmenstar1.digiDict.backup

enum class BackupFormat(@JvmField val extension: String, val shortName: String) {
    DDDB("dddb", "DDDB"),
    JSON("json", "JSON");

    companion object {
        fun fromExtensionOrNull(value: String) = when (value) {
            "dddb" -> DDDB
            "json" -> JSON
            else -> null
        }
    }
}