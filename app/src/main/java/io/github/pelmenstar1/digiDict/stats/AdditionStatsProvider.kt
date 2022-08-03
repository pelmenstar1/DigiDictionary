package io.github.pelmenstar1.digiDict.stats

import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.time.SECONDS_IN_DAY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AdditionStatsProvider {
    suspend fun compute(currentEpochSeconds: Long): AdditionStats
}

class DbAdditionStatsProvider(private val appDatabase: AppDatabase) : AdditionStatsProvider {
    private fun SupportSQLiteStatement.bindLongAndExecuteToInt(value: Long): Int {
        // Binding index is 1-based.
        bindLong(1, value)

        return simpleQueryForLong().toInt()
    }

    override suspend fun compute(currentEpochSeconds: Long): AdditionStats {
        return withContext(Dispatchers.IO) {
            val statement = appDatabase.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ?")

            statement.use {
                val last24Hours = it.bindLongAndExecuteToInt(currentEpochSeconds - SECONDS_IN_DAY)
                val last7Days = it.bindLongAndExecuteToInt(currentEpochSeconds - 7 * SECONDS_IN_DAY)
                val last31Days = it.bindLongAndExecuteToInt(currentEpochSeconds - 31 * SECONDS_IN_DAY)

                AdditionStats(last24Hours, last7Days, last31Days)
            }
        }
    }
}