package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.commonTestUtils.clearThroughReflection
import io.github.pelmenstar1.digiDict.commonTestUtils.runAndWaitForResult
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders.ManageRemoteDictionaryProvidersViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class ManageRemoteDictionaryProvidersViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        providerDao: RemoteDictionaryProviderDao = db.remoteDictionaryProviderDao(),
        statsDao: RemoteDictionaryProviderStatsDao = db.remoteDictionaryProviderStatsDao()
    ): ManageRemoteDictionaryProvidersViewModel {
        return ManageRemoteDictionaryProvidersViewModel(providerDao, statsDao)
    }

    @Test
    fun deleteTest() = runTest {
        val providerDao = db.remoteDictionaryProviderDao()
        val statsDao = db.remoteDictionaryProviderStatsDao()

        val vm = createViewModel(providerDao, statsDao)

        providerDao.insert(
            RemoteDictionaryProviderInfo(
                name = "Name",
                schema = "Schema",
                urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules()
            )
        )

        val provider = providerDao.getByName("Name")!!
        statsDao.insert(RemoteDictionaryProviderStats(provider.id, visitCount = 1))

        vm.deleteAction.runAndWaitForResult(provider)

        assertNull(providerDao.getByName("Name"))
        assertNull(statsDao.getById(provider.id))
        vm.clearThroughReflection()
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