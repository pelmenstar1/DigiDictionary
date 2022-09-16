package io.github.pelmenstar1.digiDict.backup.importing

class ImportException(val reason: Int, msg: String? = "", cause: Throwable? = null) : Exception(msg, cause) {
    companion object {
        const val REASON_DATA_VALIDATION = 0
        const val REASON_UNKNOWN_VERSION = 1
        const val REASON_INTERNAL = 2
    }
}