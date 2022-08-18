package io.github.pelmenstar1.digiDict.data

import android.database.Cursor
import androidx.room.*
import io.github.pelmenstar1.digiDict.common.generateUniqueRandomNumbers
import io.github.pelmenstar1.digiDict.common.serialization.SerializableIterable
import io.github.pelmenstar1.digiDict.common.withRemovedElementAt
import kotlinx.coroutines.flow.Flow
import kotlin.math.min
import kotlin.random.Random

@Dao
abstract class RecordDao {
    class IdExpressionMeaningRecord(
        val id: Int,
        val expression: String,
        val meaning: String
    )

    class IdBadgesRecord(
        val id: Int,
        val badges: String
    )

    @Query("SELECT count(*) FROM records")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(value: Record)

    @Insert
    abstract suspend fun insertAll(values: Array<Record>)

    @Query(
        """UPDATE records 
        SET expression=:expr, 
            meaning=:meaning,
            additionalNotes=:additionalNotes,
            dateTime=:epochSeconds,
            badges=:badges
        WHERE id=:id
        """
    )
    abstract suspend fun update(
        id: Int,
        expr: String,
        meaning: String,
        additionalNotes: String,
        epochSeconds: Long,
        badges: String
    )

    @Query("UPDATE records SET score=:newScore WHERE id=:id")
    abstract suspend fun updateScore(id: Int, newScore: Int)

    @Query("UPDATE records SET badges=:encodedBadges WHERE id=:id")
    abstract suspend fun updateBadges(id: Int, encodedBadges: String)

    @Transaction
    open suspend fun updateScores(records: Array<Record>, newScores: IntArray) {
        for (i in records.indices) {
            updateScore(records[i].id, newScores[i])
        }
    }

    @Query("DELETE FROM records WHERE id = :id")
    abstract suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM records")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM records WHERE id=:id")
    abstract suspend fun getRecordById(id: Int): Record?

    @Query("SELECT * FROM records WHERE id=:id")
    abstract fun getRecordFlowById(id: Int): Flow<Record?>

    @Query("SELECT * FROM records WHERE id IN (:ids)")
    abstract suspend fun getRecordsByIds(ids: IntArray): Array<Record>

    @Query("SELECT id, badges FROM records WHERE INSTR(badges, :encodedName) > 0")
    abstract suspend fun getRecordsByBadgeName(encodedName: String): Array<IdBadgesRecord>

    @Transaction
    open suspend fun changeRecordBadgeName(fromName: String, toName: String) {
        val records = getRecordsByBadgeName(RecordBadgeNameUtil.encode(fromName))
        for (record in records) {
            val badges = RecordBadgeNameUtil.decodeArray(record.badges)
            badges.indexOf(fromName).also {
                if (it >= 0) {
                    badges[it] = toName
                }
            }

            updateBadges(record.id, RecordBadgeNameUtil.encodeArray(badges))
        }
    }

    @Transaction
    open suspend fun deleteBadgeFromAllRecords(name: String) {
        val records = getRecordsByBadgeName(RecordBadgeNameUtil.encode(name))

        for (record in records) {
            val oldBadges = RecordBadgeNameUtil.decodeArray(record.badges)
            val badgeToDeleteIndex = oldBadges.indexOf(name)

            val newBadges = if (badgeToDeleteIndex >= 0) {
                oldBadges.withRemovedElementAt(badgeToDeleteIndex)
            } else {
                oldBadges
            }

            updateBadges(record.id, RecordBadgeNameUtil.encodeArray(newBadges))
        }
    }

    @Query("SELECT expression, meaning, additionalNotes, dateTime, score,badges FROM records")
    abstract fun getAllRecordsNoIdRaw(): Cursor

    @Query("SELECT * FROM records")
    abstract suspend fun getAllRecords(): Array<Record>

    @Query("SELECT * FROM records LEFT JOIN search_prepared_records AS spr ON records.id=spr.id")
    abstract fun getAllRecordsWithSearchInfoFlow(): Flow<Array<RecordWithSearchInfo>>

    @Query("SELECT * FROM records ORDER BY dateTime DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getRecordsLimitOffset(
        limit: Int,
        offset: Int,
    ): List<Record>

    @Query("SELECT expression FROM records")
    abstract suspend fun getAllExpressions(): Array<String>

    @Query("SELECT id, expression, meaning FROM records ORDER BY dateTime DESC LIMIT :n")
    abstract fun getLastIdExprMeaningRecordsBlocking(n: Int): Array<IdExpressionMeaningRecord>

    @Query("SELECT * FROM records WHERE expression = :expr")
    abstract suspend fun getRecordByExpression(expr: String): Record?

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

    @Transaction
    open suspend fun getRandomRecordsRegardlessScore(random: Random, requestedSize: Int): Array<Record> {
        val totalSize = count()
        val resolvedSize = min(requestedSize, totalSize)

        val indices = random.generateUniqueRandomNumbers(totalSize, resolvedSize)
        val allIds = getAllIds()

        val resultIds = IntArray(indices.size) { allIds[indices[it]] }

        return getRecordsByIds(resultIds)
    }

    @Transaction
    open suspend fun getRandomRecords(random: Random, size: Int): Array<Record> {
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

        return getRecordsByIds(ids)
    }

    @Transaction
    open suspend fun getRandomRecordsAfter(random: Random, maxSize: Int, afterEpochSeconds: Long): Array<Record> {
        val ids = getIdsAfter(afterEpochSeconds)
        ids.shuffle(random)

        val narrowedIds = IntArray(min(ids.size, maxSize))
        ids.copyInto(narrowedIds, endIndex = narrowedIds.size)

        return getRecordsByIds(narrowedIds)
    }

    fun getAllRecordsNoIdIterable(): SerializableIterable {
        val cursor = getAllRecordsNoIdRaw()

        return cursor.asRecordSerializableIterableNoId()
    }
}