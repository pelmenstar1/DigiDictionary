package io.github.pelmenstar1.digiDict.ui.resolveImportConflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.backup.TemporaryImportStorage
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConflictEntry
import io.github.pelmenstar1.digiDict.data.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ResolveImportConflictsViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    val entries = TemporaryImportStorage.conflictEntries
        ?: throw IllegalStateException("Temporary import storage is empty")

    val entriesStates = IntArray(entries.size)

    var onApplyChangesError: (() -> Unit)? = null
    var onSuccessfulApplyChanges: (() -> Unit)? = null

    fun onItemStateChanged(index: Int, state: Int) {
        entriesStates[index] = state

        val needToApplyChanges = entriesStates.all { it != ResolveImportConflictItemState.INITIAL }

        if (needToApplyChanges) {
            applyChanges()
        }
    }

    fun applyChanges() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val importedRecords = TemporaryImportStorage.importedRecords
                    ?: throw IllegalStateException("Temporary import storage is empty")

                val resolveRecordsSequence = entries.asSequence().mapIndexed { index, entry ->
                    resolveConflict(entry, entriesStates[index])
                }

                appDatabase.withTransaction {
                    recordDao.insertAll(importedRecords)
                    recordDao.updateAsResolveConflictAll(resolveRecordsSequence)
                }

                // Allow GC to do its work.
                TemporaryImportStorage.importedRecords = null
                TemporaryImportStorage.conflictEntries = null

                withContext(Dispatchers.Main) {
                    onSuccessfulApplyChanges?.invoke()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onApplyChangesError?.invoke()
                }
            }
        }
    }

    private fun resolveConflict(entry: ConflictEntry, state: Int): Record {
        return when (state) {
            ResolveImportConflictItemState.ACCEPT_OLD -> {
                Record(
                    id = entry.id,
                    expression = entry.expression,
                    rawMeaning = entry.oldRawMeaning,
                    additionalNotes = entry.oldAdditionalNotes,
                    score = entry.oldScore,
                    epochSeconds = entry.oldEpochSeconds
                )
            }
            ResolveImportConflictItemState.ACCEPT_NEW -> {
                Record(
                    id = entry.id,
                    expression = entry.expression,
                    rawMeaning = entry.newRawMeaning,
                    additionalNotes = entry.newAdditionalNotes,
                    score = entry.newScore,
                    epochSeconds = entry.newEpochSeconds
                )
            }
            ResolveImportConflictItemState.MERGE -> {
                val oldMeaning = ComplexMeaning.parse(entry.oldRawMeaning)
                val newMeaning = ComplexMeaning.parse(entry.newRawMeaning)

                val mergedMeaning = oldMeaning.mergedWith(newMeaning)
                val mergedAdditionalNotes = entry.oldAdditionalNotes.ifEmpty {
                    entry.newAdditionalNotes
                }

                Record(
                    id = entry.id,
                    expression = entry.expression,
                    rawMeaning = mergedMeaning.rawText,
                    additionalNotes = mergedAdditionalNotes,
                    score = maxOf(entry.oldScore, entry.newScore),
                    epochSeconds = maxOf(entry.oldEpochSeconds, entry.newEpochSeconds)
                )
            }
            else -> throw IllegalArgumentException("invalid state ($state)")
        }
    }
}