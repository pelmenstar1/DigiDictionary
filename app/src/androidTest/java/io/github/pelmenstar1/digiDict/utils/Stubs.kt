package io.github.pelmenstar1.digiDict.utils

import android.database.Cursor
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.stats.AdditionStats
import io.github.pelmenstar1.digiDict.stats.AdditionStatsProvider
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.Flow

open class RemoteDictionaryProviderDaoStub : RemoteDictionaryProviderDao {
    override suspend fun insert(value: RemoteDictionaryProviderInfo) {
    }

    override suspend fun delete(value: RemoteDictionaryProviderInfo) {
    }

    override fun getAllFlow(): Flow<Array<RemoteDictionaryProviderInfo>> {
        throw NotImplementedError()
    }

    override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
        throw NotImplementedError()
    }

    override suspend fun getById(id: Int): RemoteDictionaryProviderInfo? {
        throw NotImplementedError()
    }

    override suspend fun getByName(name: String): RemoteDictionaryProviderInfo? {
        throw NotImplementedError()
    }
}

open class RecordDaoStub : RecordDao() {
    override suspend fun count(): Int {
        throw NotImplementedError()
    }

    override suspend fun insert(value: Record) {
    }

    override suspend fun insertAll(values: List<Record>) {
    }

    override suspend fun update(
        id: Int,
        newExpression: String,
        newMeaning: String,
        newAdditionalNotes: String,
        newDateTimeEpochSeconds: Long
    ) {
    }

    override suspend fun updateAsResolveConflict(
        id: Int,
        newMeaning: String,
        newAdditionalNotes: String,
        newDateTimeEpochSeconds: Long,
        newScore: Int
    ) {
    }

    override suspend fun updateScore(id: Int, newScore: Int) {
    }

    override suspend fun deleteById(id: Int): Int {
        throw NotImplementedError()
    }

    override suspend fun getRecordById(id: Int): Record? {
        throw NotImplementedError()
    }

    override fun getRecordFlowById(id: Int): Flow<Record?> {
        throw NotImplementedError()
    }

    override suspend fun getRecordsByIds(ids: IntArray): Array<Record> {
        throw NotImplementedError()
    }

    override suspend fun getAllRecordsOrderByDateTime(): Array<Record> {
        throw NotImplementedError()
    }

    override fun getAllRecordsNoIdRaw(): Cursor {
        throw NotImplementedError()
    }

    override suspend fun getAllRecords(): Array<Record> {
        throw NotImplementedError()
    }

    override suspend fun getRecordsLimitOffset(limit: Int, offset: Int): List<Record> {
        throw NotImplementedError()
    }

    override suspend fun getAllExpressions(): Array<String> {
        throw NotImplementedError()
    }

    override fun getLastIdExprMeaningRecordsBlocking(n: Int): Array<IdExpressionMeaningRecord> {
        throw NotImplementedError()
    }

    override suspend fun getRecordByExpression(expr: String): Record? {
        throw NotImplementedError()
    }

    override suspend fun getIdsWithPositiveScore(): IntArray {
        throw NotImplementedError()
    }

    override suspend fun getIdsWithNegativeScore(): IntArray {
        throw NotImplementedError()
    }

    override suspend fun getIdsAfter(epochSeconds: Long): IntArray {
        throw NotImplementedError()
    }
}

object AppWidgetUpdaterStub : AppWidgetUpdater {
    override fun updateAllWidgets() {
    }
}

object AdditionStatsProviderStub : AdditionStatsProvider {
    override fun compute(currentEpochSeconds: Long): AdditionStats {
        throw RuntimeException()
    }
}