package io.github.pelmenstar1.digiDict.data

import android.database.Cursor
import androidx.room.*
import io.github.pelmenstar1.digiDict.common.generateUniqueRandomNumbers
import io.github.pelmenstar1.digiDict.common.mapToArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.min
import kotlin.random.Random

@Dao
abstract class RecordDao {
    class IdExpressionMeaningRecord(
        val id: Int,
        val expression: String,
        val meaning: String
    )

    @Query("SELECT count(*) FROM records")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(value: Record)

    @Insert
    abstract suspend fun insertAll(values: Array<out Record>)

    @Query(
        """UPDATE records 
        SET expression=:expr, 
            meaning=:meaning,
            additionalNotes=:additionalNotes,
            dateTime=:epochSeconds
        WHERE id=:id
        """
    )
    abstract suspend fun update(
        id: Int,
        expr: String,
        meaning: String,
        additionalNotes: String,
        epochSeconds: Long
    )

    @Query("UPDATE records SET score=:newScore WHERE id=:id")
    abstract suspend fun updateScore(id: Int, newScore: Int)

    @Transaction
    open suspend fun updateScores(records: Array<out EntityWithPrimaryKeyId>, newScores: IntArray) {
        for (i in records.indices) {
            updateScore(records[i].id, newScores[i])
        }
    }

    @Query("DELETE FROM records WHERE id = :id")
    abstract suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM records")
    abstract suspend fun deleteAll()

    @Query(GET_RECORD_BADGES_BY_RECORD_ID_QUERY)
    abstract suspend fun getRecordBadgesByRecordId(id: Int): Array<RecordBadgeInfo>

    @Query(GET_RECORD_BADGES_BY_RECORD_ID_QUERY)
    abstract fun getRecordBadgesFlowByRecordId(id: Int): Flow<Array<RecordBadgeInfo>>

    @Query(GET_RECORD_BY_ID_QUERY)
    abstract suspend fun getRecordById(id: Int): Record?

    suspend fun getRecordWithBadgesById(id: Int): RecordWithBadges? {
        val record = getRecordById(id)

        return record?.let {
            val badges = getRecordBadgesByRecordId(id)

            RecordWithBadges.create(record, badges)
        }
    }

    @Query(GET_RECORD_BY_ID_QUERY)
    abstract fun getRecordFlowById(id: Int): Flow<Record?>

    fun getRecordWithBadgesFlowById(id: Int): Flow<RecordWithBadges?> {
        val recordFlow = getRecordFlowById(id)
        val badgesFlow = getRecordBadgesFlowByRecordId(id)

        return recordFlow.combine(badgesFlow) { record, badges ->
            record?.let { RecordWithBadges.create(it, badges) }
        }
    }

    @Query("SELECT id, expression, meaning, score FROM records WHERE id IN (:ids)")
    abstract suspend fun getConciseRecordsByIds(ids: IntArray): Array<ConciseRecord>

    suspend fun getConciseRecordsWithBadgesByIds(ids: IntArray): Array<ConciseRecordWithBadges> {
        val records = getConciseRecordsByIds(ids)

        return records.mapToArray {
            val badges = getRecordBadgesByRecordId(it.id)

            ConciseRecordWithBadges.create(it, badges)
        }
    }

    @Query("SELECT expression, meaning, additionalNotes, dateTime, score FROM records")
    abstract fun getAllRecordsNoIdRaw(): Cursor

    @Query("SELECT * FROM records")
    abstract suspend fun getAllRecords(): Array<Record>

    @Query("SELECT id, expression, meaning, score FROM records")
    abstract suspend fun getAllConciseRecords(): Array<ConciseRecord>

    suspend fun getAllConciseRecordsWithBadges(): Array<ConciseRecordWithBadges> {
        val records = getAllConciseRecords()

        return records.mapToArray {
            val badges = getRecordBadgesByRecordId(it.id)

            ConciseRecordWithBadges.create(it, badges)
        }
    }

    @Query(
        """
        SELECT id, expression, meaning, score 
        FROM records
        ORDER BY dateTime DESC 
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getConciseRecordsLimitOffset(
        limit: Int,
        offset: Int,
    ): Array<ConciseRecord>

    suspend fun getConciseRecordsWithBadgesLimitOffset(
        limit: Int,
        offset: Int
    ): List<ConciseRecordWithBadges> {
        val records = getConciseRecordsLimitOffset(limit, offset)

        return records.map {
            val badges = getRecordBadgesByRecordId(it.id)

            ConciseRecordWithBadges.create(it, badges)
        }
    }

    @Query("SELECT expression FROM records")
    abstract suspend fun getAllExpressions(): Array<String>

    @Query("SELECT id, expression, meaning FROM records ORDER BY dateTime DESC LIMIT :n")
    abstract fun getLastIdExprMeaningRecordsBlocking(n: Int): Array<IdExpressionMeaningRecord>

    @Query("SELECT * FROM records WHERE expression = :expr")
    abstract suspend fun getRecordByExpression(expr: String): Record?

    suspend fun getRecordWithBadgesByExpression(expr: String): RecordWithBadges? {
        return getRecordByExpression(expr)?.let {
            val badges = getRecordBadgesByRecordId(it.id)

            RecordWithBadges.create(it, badges)
        }
    }

    @Query("SELECT id FROM records WHERE expression=:expr")
    abstract suspend fun getRecordIdByExpression(expr: String): Int?

    @Query("SELECT id FROM records")
    abstract suspend fun getAllIds(): IntArray

    @Query("SELECT id FROM records WHERE score >= 0")
    abstract suspend fun getIdsWithPositiveScore(): IntArray

    @Query("SELECT id FROM records WHERE score < 0")
    abstract suspend fun getIdsWithNegativeScore(): IntArray

    @Query("SELECT id FROM records WHERE dateTime >= :epochSeconds")
    abstract suspend fun getIdsAfter(epochSeconds: Long): IntArray

    suspend fun getRandomConciseRecordsWithBadgesRegardlessScore(
        random: Random,
        requestedSize: Int
    ): Array<ConciseRecordWithBadges> {
        val totalSize = count()
        val resolvedSize = min(requestedSize, totalSize)

        val indices = random.generateUniqueRandomNumbers(totalSize, resolvedSize)
        val allIds = getAllIds()

        val resultIds = IntArray(indices.size) { allIds[indices[it]] }

        return getConciseRecordsWithBadgesByIds(resultIds)
    }

    @Transaction
    open suspend fun getRandomConciseRecordsWithBadges(random: Random, size: Int): Array<ConciseRecordWithBadges> {
        val halfSize = size / 2
        if (halfSize * 2 != size) {
            throw IllegalArgumentException("Size must be even")
        }

        val idsWithPositiveScore = getIdsWithPositiveScore()
        val idsWithNegativeScore = getIdsWithNegativeScore()

        val indicesWithNegativeScore = random.generateUniqueRandomNumbers(
            upperBound = idsWithNegativeScore.size,
            size = min(idsWithNegativeScore.size, halfSize)
        )

        val indicesWithPositiveScore = random.generateUniqueRandomNumbers(
            upperBound = idsWithPositiveScore.size,
            size = min(
                size - indicesWithNegativeScore.size,
                min(idsWithPositiveScore.size, halfSize)
            )
        )

        val posScoreSize = indicesWithPositiveScore.size
        val negScoreSize = indicesWithNegativeScore.size
        val ids = IntArray(posScoreSize + negScoreSize) { i ->
            if (i < posScoreSize) {
                idsWithPositiveScore[indicesWithPositiveScore[i]]
            } else {
                idsWithNegativeScore[indicesWithNegativeScore[i - posScoreSize]]
            }
        }

        ids.shuffle(random)

        return getConciseRecordsWithBadgesByIds(ids)
    }

    @Transaction
    open suspend fun getRandomConciseRecordsWithBadgesAfter(
        random: Random,
        maxSize: Int,
        afterEpochSeconds: Long
    ): Array<ConciseRecordWithBadges> {
        val ids = getIdsAfter(afterEpochSeconds)
        ids.shuffle(random)

        val narrowedIds = IntArray(min(ids.size, maxSize))
        ids.copyInto(narrowedIds, endIndex = narrowedIds.size)

        return getConciseRecordsWithBadgesByIds(narrowedIds)
    }

    companion object {
        private const val GET_RECORD_BADGES_BY_RECORD_ID_QUERY = """
        SELECT rb.id as id, rb.name as name, rb.outlineColor as outlineColor 
        FROM record_badges AS rb 
        WHERE rb.id IN (SELECT rbr.badgeId FROM record_to_badge_relations AS rbr WHERE rbr.recordId=:id) 
        """

        private const val GET_RECORD_BY_ID_QUERY = "SELECT * FROM records WHERE id=:id"
    }
}