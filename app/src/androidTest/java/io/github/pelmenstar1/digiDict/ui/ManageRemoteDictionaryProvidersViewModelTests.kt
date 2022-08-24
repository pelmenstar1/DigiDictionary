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
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNull
import kotlin.test.fail

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

        vm.onDeleteError.handler = { fail() }

        vm.delete(provider)
        delay(1000)

        assertNull(providerDao.getByName("Name"))
        assertNull(statsDao.getById(provider.id))
        vm.clearThroughReflection()
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