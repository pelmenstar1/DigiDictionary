package io.github.pelmenstar1.digiDict.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStats
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RemoteDictionaryProviderDaoTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createProvider(name: String): RemoteDictionaryProviderInfo {
        return RemoteDictionaryProviderInfo(
            id = 0,
            name,
            "https://a.com/\$query$",
            urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules()
        )
    }

    @Test
    fun getMostUsedProviders_whenNoStats() = runTest {
        val providerDao = db.remoteDictionaryProviderDao()

        providerDao.insert(createProvider("P1"))
        providerDao.insert(createProvider("P2"))

        // providers and mostUsedProviders must be equal, but can be placed in different order.
        // To resolve this issue, convert both to sets.
        val providers = providerDao.getAll().toSet()
        val mostUsedProviders = providerDao.getMostUsedProviders().toSet()

        assertEquals(providers, mostUsedProviders)
    }

    @Test
    fun getMostUsedProviders_providersWithoutStatsShouldBeLast() = runTest {
        val providerDao = db.remoteDictionaryProviderDao()
        val statsDao = db.remoteDictionaryProviderStatsDao()

        providerDao.insert(createProvider("P1"))
        providerDao.insert(createProvider("P2"))
        providerDao.insert(createProvider("P3"))
        providerDao.insert(createProvider("P4"))

        val p1 = providerDao.getByName("P1")!!
        val p2 = providerDao.getByName("P2")!!
        val p3 = providerDao.getByName("P3")!!
        val p4 = providerDao.getByName("P4")!!

        statsDao.insert(RemoteDictionaryProviderStats(p1.id, visitCount = 2))
        statsDao.insert(RemoteDictionaryProviderStats(p3.id, visitCount = 100))
        statsDao.insert(RemoteDictionaryProviderStats(p4.id, visitCount = 50))

        val mostUsedProviders = providerDao.getMostUsedProviders()

        assertEquals(4, mostUsedProviders.size)

        assertEquals(p3, mostUsedProviders[0])
        assertEquals(p4, mostUsedProviders[1])
        assertEquals(p1, mostUsedProviders[2])
        assertEquals(p2, mostUsedProviders[3])
    }

    companion object {
        private lateinit var db: AppDatabase

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()

            db = AppDatabaseUtils.createTestDatabase(context)
        }

        @After
        fun after() {
            db.close()
        }
    }
}