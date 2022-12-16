package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderViewModel
import io.github.pelmenstar1.digiDict.utils.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

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

    private suspend fun assertInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        errorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>,
        expectedMsg: AddRemoteDictionaryProviderMessage,
        validityField: AddRemoteDictionaryProviderViewModel.Companion.() -> ValidityFlow.Field,
    ) {
        val field = validityField(AddRemoteDictionaryProviderViewModel.Companion)

        assertFalse(vm.validityFlow.waitForComputedAndReturnIsValid(field))
        assertEquals(expectedMsg, errorFlow.first())
    }

    private suspend fun assertValidState(
        vm: AddRemoteDictionaryProviderViewModel,
        errorFlow: StateFlow<AddRemoteDictionaryProviderMessage?>,
        validityField: AddRemoteDictionaryProviderViewModel.Companion.() -> ValidityFlow.Field,
    ) {
        val field = validityField(AddRemoteDictionaryProviderViewModel.Companion)

        assertTrue(vm.validityFlow.waitForComputedAndReturnIsValid(field))
        assertNull(errorFlow.first())
    }

    private suspend fun assertNameInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        expectedMsg: AddRemoteDictionaryProviderMessage
    ) {
        assertInvalidState(vm, vm.nameErrorFlow, expectedMsg) { nameValidityField }
    }

    private suspend fun assertSchemaInvalidState(
        vm: AddRemoteDictionaryProviderViewModel,
        expectedMsg: AddRemoteDictionaryProviderMessage
    ) {
        assertInvalidState(vm, vm.schemaErrorFlow, expectedMsg) { schemaValidityField }
    }

    private suspend fun assertNameValidState(vm: AddRemoteDictionaryProviderViewModel) {
        assertValidState(vm, vm.nameErrorFlow) { nameValidityField }
    }

    private suspend fun assertSchemaValidState(vm: AddRemoteDictionaryProviderViewModel) {
        assertValidState(vm, vm.schemaErrorFlow) { schemaValidityField }
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
        dao.insert(createProvider("Name1", "Schema1"))
        dao.insert(createProvider("Name2", "Schema2"))

        val vm = createViewModel(dao)

        vm.use {
            vm.name = "Name3"

            assertNameValidState(vm)
        }
    }

    @Test
    fun schemaIsInvalidIfItAlreadyExistsTest() = runTest {
        val dao = db.remoteDictionaryProviderDao()

        // URL validity, $query$ checks should be run first, so URL's should be valid
        dao.insert(createProvider("Name1", "https://a.com/\$query$"))
        dao.insert(createProvider("Name2", "https://b.com/\$query$"))
        dao.insert(createProvider("Name3", "https://c.com/\$query$"))

        val vm = createViewModel(dao)

        suspend fun testCase(schema: String) {
            vm.schema = schema

            assertSchemaInvalidState(vm, AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS)
        }

        vm.use {
            testCase("https://a.com/\$query$")
            testCase("https://b.com/\$query$")
            testCase("https://c.com/\$query$")
        }
    }

    @Test
    fun schemaIsValidIfItDoesNotExists() = runTest {
        val dao = db.remoteDictionaryProviderDao()

        // URL validity, $query$ checks should be run first, so URL's should be valid
        dao.insert(createProvider("Name1", "https://a.com/\$query$"))
        dao.insert(createProvider("Name2", "https://b.com/\$query$"))

        val vm = createViewModel(dao)

        vm.use {
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

        vm.use {
            vm.name = "Provider1"
            vm.schema = "https://a.com/\$query$"
            vm.spaceReplacement = '_'

            vm.addAction.runAndWaitForResult()

            val info = db.remoteDictionaryProviderDao().getByName("Provider1")!!

            assertEquals("https://a.com/\$query$", info.schema)
            assertEquals('_', info.urlEncodingRules.spaceReplacement)
        }
    }

    @Test
    fun addTest_validityIsInvalidAndComputed() {
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun insert(value: RemoteDictionaryProviderInfo) {
                throw Exception()
            }

            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                return emptyArray()
            }
        })

        vm.use {
            // Make validity invalid explicitly
            vm.name = ""
            vm.schema = ""

            vm.add()
        }
    }

    @Test
    fun addTest_validityIsNotComputedThenComputed() = runTest {
        var throwOnInsert = true
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun insert(value: RemoteDictionaryProviderInfo) {
                if (throwOnInsert) {
                    throw Exception("Should not be here")
                }
            }

            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                return emptyArray()
            }
        })

        vm.use {
            vm.validityFlow.mutate {
                disable(AddRemoteDictionaryProviderViewModel.nameValidityField, isComputed = false)
                disable(AddRemoteDictionaryProviderViewModel.schemaValidityField, isComputed = false)
            }
            vm.add()

            Thread.sleep(100) // emulate some work

            throwOnInsert = false

            vm.validityFlow.mutate {
                enable(AddRemoteDictionaryProviderViewModel.nameValidityField)
                enable(AddRemoteDictionaryProviderViewModel.schemaValidityField)
            }

            vm.addAction.waitForResult()
        }
    }

    @Test
    fun validityCheckErrorTest() = runTest {
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                // Simulate DB error.
                throw RuntimeException()
            }
        })

        vm.use {
            // Fetching the data is async, so we need to wait
            Thread.sleep(100)
            assertNotNull(vm.validityCheckErrorFlow.first())
        }
    }

    // TODO: Fix the test
    @Test
    @Ignore("Disabled until the investigation of the reason it's failing")
    fun isInputsEnabledFlowTest() = runTest {
        var isFirst = true
        val vm = createViewModel(dao = object : RemoteDictionaryProviderDaoStub() {
            override suspend fun getAll(): Array<RemoteDictionaryProviderInfo> {
                if (isFirst) {
                    isFirst = false

                    // Simulate DB error.
                    throw RuntimeException()
                } else {
                    // Result doesn't matter in this test, anything can be returned.
                    return emptyArray()
                }
            }
        })

        // Trigger name check and fetching all providers.
        vm.name = "123"

        // The checking is expected to fail due to an exception and in that case, inputs should be dsiabled.
        assertFalse(vm.isInputEnabledFlow.first())

        vm.restartValidityCheck()

        // We can't determine whether checking is actually started, wait some time.
        Thread.sleep(300)

        // As we restarted checking the name and in this time getAll should return without an exception, inputs should be enabled.
        assertTrue(vm.isInputEnabledFlow.first())
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