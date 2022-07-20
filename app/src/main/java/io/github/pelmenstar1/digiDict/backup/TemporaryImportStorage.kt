package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.data.ConflictEntry
import io.github.pelmenstar1.digiDict.data.Record

object TemporaryImportStorage {
    /**
     * Database should not contain this records as they are expected to be inserted to the DB.
     * In other words, no expressions in the DB shouldn't match any expression in importedRecords as it would
     * break the UNIQUE expression contract in the DB.
     */
    var importedRecords: List<Record>? = null
    var conflictEntries: List<ConflictEntry>? = null
}