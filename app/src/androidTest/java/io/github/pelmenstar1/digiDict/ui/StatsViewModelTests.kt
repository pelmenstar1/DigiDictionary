package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.stats.AdditionStats
import io.github.pelmenstar1.digiDict.stats.CommonStats
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.ui.stats.StatsViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.assertEventHandlerOnMainThread
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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

    @Test
    fun onLoadErrorCalledOnMainThread() = runTest {
        var isFirstCall = true
        val vm = createStatsViewModel(commonStatsProvider = object : CommonStatsProvider {
            override suspend fun compute(currentEpochSeconds: Long): CommonStats {
                if (isFirstCall) {
                    isFirstCall = false

                    return CommonStats(0, AdditionStats(0, 0, 0))
                } else {
                    throw RuntimeException()
                }
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onLoadError) { computeStats() }
    }

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