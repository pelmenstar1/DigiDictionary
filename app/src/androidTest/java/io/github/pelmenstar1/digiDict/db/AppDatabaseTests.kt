package io.github.pelmenstar1.digiDict.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTests {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun onCreateShouldAddDefaultRdpsTest() = runTest {
        val db = AppDatabase.createInMemory(context)

        val actualProviders = db.remoteDictionaryProviderDao().getAll()
        assertContentEqualsNoId(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS, actualProviders)

        db.close()
    }
}