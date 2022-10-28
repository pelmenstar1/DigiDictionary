package io.github.pelmenstar1.digiDict.stats

import io.github.pelmenstar1.digiDict.data.AppDatabase

interface CommonStatsProvider {
    fun compute(currentEpochSeconds: Long): CommonStats
}

class DbCommonStatsProvider(
    private val appDatabase: AppDatabase
) : CommonStatsProvider {
    private val additionStatsProvider = DbAdditionStatsProvider(appDatabase)

    override fun compute(currentEpochSeconds: Long): CommonStats {
        val count = compileCountStatement().use {
            it.simpleQueryForLong().toInt()
        }
        val additionStats = additionStatsProvider.compute(currentEpochSeconds)

        return CommonStats(count, additionStats)
    }

    private fun compileCountStatement() = appDatabase.compileStatement("SELECT COUNT(*) FROM records")
}