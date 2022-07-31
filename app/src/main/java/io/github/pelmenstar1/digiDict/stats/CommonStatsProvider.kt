package io.github.pelmenstar1.digiDict.stats

import io.github.pelmenstar1.digiDict.data.AppDatabase

interface CommonStatsProvider {
    suspend fun compute(currentEpochSeconds: Long): CommonStats
}

class DbCommonStatsProvider(
    appDatabase: AppDatabase
) : CommonStatsProvider {
    private val recordDao = appDatabase.recordDao()
    private val additionStatsProvider = DbAdditionStatsProvider(appDatabase)

    override suspend fun compute(currentEpochSeconds: Long): CommonStats {
        val count = recordDao.count()
        val additionStats = additionStatsProvider.compute(currentEpochSeconds)

        return CommonStats(count, additionStats)
    }
}