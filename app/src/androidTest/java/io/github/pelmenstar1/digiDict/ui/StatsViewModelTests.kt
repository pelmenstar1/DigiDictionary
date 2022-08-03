package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.ui.stats.StatsViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("There are no tests for now")
class StatsViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createStatsViewModel(
        commonStatsProvider: CommonStatsProvider = DbCommonStatsProvider(db),
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider
    ): StatsViewModel {
        return StatsViewModel(commonStatsProvider, currentEpochSecondsProvider)
    }

    // There are no tests here, but maybe in future there will be.

    companion object {
        private lateinit var db: AppDatabase

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()

            db = AppDatabaseUtils.createTestDatabase(context)
        }

        @AfterClass
        @JvmStatic
        fun after() {
            db.close()
        }
    }
}