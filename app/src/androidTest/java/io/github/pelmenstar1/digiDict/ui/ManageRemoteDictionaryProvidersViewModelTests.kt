package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders.ManageRemoteDictionaryProvidersViewModel
import io.github.pelmenstar1.digiDict.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

        vm.onDeleteError.handler = {
            assertTrue(false)
        }

        vm.delete(provider)
        delay(500)

        assertNull(providerDao.getByName("Name"))
        assertNull(statsDao.getById(provider.id))
        vm.clearThroughReflection()
    }

    @Test
    fun onLoadingErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(providerDao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                throw RuntimeException()
            }

            override fun getAllFlow(): Flow<Array<RemoteDictionaryProviderInfo>> {
                return emptyFlow()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onLoadingError) { vm.loadProviders() }
    }

    @Test
    fun onDeleteErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(providerDao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun delete(value: RemoteDictionaryProviderInfo) {
                throw RuntimeException()
            }

            override fun getAllFlow(): Flow<Array<RemoteDictionaryProviderInfo>> {
                return emptyFlow()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onDeleteError) {
            vm.delete(EMPTY_PROVIDER)
        }
    }

    @Suppress("OPT_IN_USAGE")
    @Test
    fun loadProvidersTest() = runTest {
        val expectedArray = arrayOf(EMPTY_PROVIDER)
        val vm = createViewModel(providerDao = object : RemoteDictionaryProviderDaoStub() {
            override fun getAllFlow(): Flow<Array<RemoteDictionaryProviderInfo>> {
                return emptyFlow()
            }

            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                return expectedArray
            }
        })

        vm.use {
            vm.loadProviders()
            val actualArray = vm.providersFlow.filterNotNull().first()

            assertEquals(expectedArray, actualArray)
        }
    }

    companion object {
        private val EMPTY_PROVIDER = RemoteDictionaryProviderInfo(
            name = "",
            schema = "",
            urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules()
        )

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