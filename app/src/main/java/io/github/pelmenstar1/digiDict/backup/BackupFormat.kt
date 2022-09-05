package io.github.pelmenstar1.digiDict.backup

enum class BackupFormat(@JvmField val extension: String) {
    DDDB("dddb"),
    JSON("json");

    companion object {
        fun fromExtension(value: String) = when (value) {
            "dddb" -> DDDB
            "json" -> JSON
            else -> throw RuntimeException("Invalid extension ($value)")
        }
    }
}