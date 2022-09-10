package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecordToBadgeRelationDao {
    @Insert
    suspend fun insert(value: RecordToBadgeRelation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(value: RecordToBadgeRelation)

    @Insert
    suspend fun insertAll(values: Array<out RecordToBadgeRelation>)

    @Query("DELETE FROM record_to_badge_relations WHERE recordId=:recordId AND badgeId=:badgeId")
    fun delete(recordId: Int, badgeId: Int)

    @Query("DELETE FROM record_to_badge_relations WHERE badgeId=:badgeId")
    suspend fun deleteAllBadgeRelations(badgeId: Int)

    @Query("DELETE FROM record_to_badge_relations WHERE recordId=:recordId")
    suspend fun deleteAllByRecordId(recordId: Int)

    @Query("SELECT * FROM record_to_badge_relations ORDER BY badgeId ASC")
    suspend fun getAllOrderByBadgeIdAsc(): Array<RecordToBadgeRelation>

    @Query("SELECT * FROM record_to_badge_relations WHERE badgeId=:badgeId")
    suspend fun getByBadgeId(badgeId: Int): Array<RecordToBadgeRelation>
}