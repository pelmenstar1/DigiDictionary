package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderViewModel
import io.github.pelmenstar1.digiDict.utils.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AddRemoteDictionaryProviderViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        dao: RemoteDictionaryProviderDao = db.remoteDictionaryProviderDao()
    ): AddRemoteDictionaryProviderViewModel {
        return AddRemoteDictionaryProviderViewModel(dao)
    }

    private fun createProvider(name: String, schema: String): RemoteDictionaryProviderInfo {
        return RemoteDictionaryProviderInfo(
            name = name,
            schema = schema,
            urlEncodingRules = RemoteDictionaryProviderInfo.UrlEncodingRules()
        )
    }

    private suspend fun AddRemoteDictionaryProviderViewModel.getValidity(): Int {
        return validityFlow
            .filterNotNull()
            .filter { (it and AddRemoteDictionaryProviderViewModel.NOT_CHOSEN_MASK) == 0 }
            .first()
    }

    private suspend fun assertInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        errorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>,
        expectedMsg: AddRemoteDictionaryProviderMessage,
        validityMask: AddRemoteDictionaryProviderViewModel.Companion.() -> Int
    ) {
        val validity = vm.getValidity()

        assertEquals(0, validity and validityMask(AddRemoteDictionaryProviderViewModel.Companion))
        assertEquals(expectedMsg, errorFlow.first())
    }

    private suspend fun assertValidState(
        vm: AddRemoteDictionaryProviderViewModel,
        errorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>,
        validityMask: AddRemoteDictionaryProviderViewModel.Companion.() -> Int
    ) {
        val validity = vm.getValidity()

        assertNotEquals(0, validity and validityMask(AddRemoteDictionaryProviderViewModel.Companion))
        assertNull(errorFlow.first())
    }

    private suspend fun assertNameInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        expectedMsg: AddRemoteDictionaryProviderMessage
    ) {
        assertInvalidState(vm, vm.nameErrorFlow, expectedMsg) { NAME_VALIDITY_BIT }
    }

    private suspend fun assertSchemaInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        expectedMsg: AddRemoteDictionaryProviderMessage
    ) {
        assertInvalidState(vm, vm.schemaErrorFlow, expectedMsg) { SCHEMA_VALIDITY_BIT }
    }

    private suspend fun assertNameValidState(vm: AddRemoteDictionaryProviderViewModel) {
        assertValidState(vm, vm.nameErrorFlow) { NAME_VALIDITY_BIT }
    }

    private suspend fun assertSchemaValidState(vm: AddRemoteDictionaryProviderViewModel) {
        assertValidState(vm, vm.schemaErrorFlow) { SCHEMA_VALIDITY_BIT }
    }

    @Test
    fun nameIsInvalidIfEmptyTest() = runTest {
        val vm = createViewModel()

        vm.use {
            vm.name = ""

            assertNameInvalidState(vm, AddRemoteDictionaryProviderMessage.EMPTY_TEXT)
        }
    }

    @Test
    fun schemaIsInvalidIfEmptyTest() = runTest {
        val vm = createViewModel()

        vm.use {
            vm.name = ""

            assertSchemaInvalidState(vm, AddRemoteDictionaryProviderMessage.EMPTY_TEXT)
        }
    }

    @Test
    fun nameIsInvalidIfItAlreadyExistsTest() = runTest {
        val dao = db.remoteDictionaryProviderDao()
        dao.insert(createProvider("Name1", "Schema1"))
        dao.insert(createProvider("Name2", "Schema2"))

        val vm = createViewModel(dao)

        suspend fun testCase(name: String) {
            vm.name = name

            assertNameInvalidState(vm, AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS)
        }

        vm.use {
            testCase("Name1")
            testCase("Name2")
        }
    }

    @Test
    fun nameIsValidIfItDoesNotExistsTest() = runTest {
        val dao = db.remoteDictionaryProviderDao()
        val vm = createViewModel(dao)

        vm.use {
            dao.insert(createProvider("Name1", "Schema1"))
            dao.insert(createProvider("Name2", "Schema2"))

            vm.name = "Name3"

            assertNameValidState(vm)
        }
    }

    @Test
    fun schemaIsInvalidIfItAlreadyExistsTest() = runTest {
        val dao = db.remoteDictionaryProviderDao()
        val vm = createViewModel(dao)

        suspend fun testCase(schema: String) {
            vm.schema = schema

            assertSchemaInvalidState(vm, AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS)
        }

        vm.use {
            // URL validity, $query$ checks should be run first, so URL's should be valid
            dao.insert(createProvider("Name1", "https://a.com/\$query$"))
            dao.insert(createProvider("Name2", "https://b.com/\$query$"))
            dao.insert(createProvider("Name3", "https://c.com/\$query$"))

            testCase("https://a.com/\$query$")
            testCase("https://b.com/\$query$")
            testCase("https://c.com/\$query$")
        }
    }

    @Test
    fun schemaIsValidIfItDoesNotExists() = runTest {
        val dao = db.remoteDictionaryProviderDao()
        val vm = createViewModel(dao)

        vm.use {
            // URL validity, $query$ checks should be run first, so URL's should be valid
            dao.insert(createProvider("Name1", "https://a.com/\$query$"))
            dao.insert(createProvider("Name2", "https://b.com/\$query$"))

            vm.schema = "https://c.com/\$query$"

            assertSchemaValidState(vm)
        }
    }

    @Test
    fun schemaIsInvalidIfItDoesNotHaveQuery() = runTest {
        val vm = createViewModel()

        suspend fun testCase(schema: String) {
            vm.schema = schema

            assertSchemaInvalidState(vm, AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER)
        }

        vm.use {
            testCase("https://a.com")
            testCase("https://a.com/query")
            testCase("https://a.com/\$query")
            testCase("https://a.com/query$")
            testCase("https://a.com/\$q$")
        }
    }

    @Test
    fun addTest() = runTest {
        val vm = createViewModel()

        vm.name = "Provider1"
        vm.schema = "https://a.com/\$query$"
        vm.spaceReplacement = '_'

        vm.add()

        vm.onAdditionError.handler = {
            // Crash
            assertTrue(false)
        }

        vm.onSuccessfulAddition.setHandlerAndWait {
            launch {
                val info = db.remoteDictionaryProviderDao().getByName("Provider1")!!

                assertEquals("https://a.com/\$query$", info.schema)
                assertEquals('_', info.urlEncodingRules.spaceReplacement)

                vm.clearThroughReflection()
            }
        }
    }

    @Test
    fun onAdditionErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun insert(value: RemoteDictionaryProviderInfo) {
                // Simulate DB error.
                throw RuntimeException()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onAdditionError, triggerAction = { add() })
    }

    @Test
    fun onSuccessfulAdditionCalledOnMainThreadTest() = runTest {
        val vm = createViewModel()

        assertEventHandlerOnMainThread(vm, vm.onSuccessfulAddition, triggerAction = { add() })
    }

    @Test
    fun onValidityCheckErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                // Simulate DB error.
                throw RuntimeException()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onValidityCheckError, triggerAction = { schema = "123" })
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