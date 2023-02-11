package io.github.pelmenstar1.digiDict.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.EventInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class EventDaoTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun isAllEventsEndedFlowTest() = runTest {
        val db = AppDatabase.createInMemory(context)
        val eventDao = db.eventDao()
        val flow = eventDao.isAllEventsEndedFlow()

        // db is empty, so there's no events that aren't ended
        assertTrue(flow.first())

        eventDao.insert(EventInfo(name = "123", startEpochSeconds = 1, endEpochSeconds = 2))
        assertTrue(flow.first())

        eventDao.insert(EventInfo(name = "324", startEpochSeconds = 3, endEpochSeconds = -1))
        assertFalse(flow.first())

        eventDao.insert(EventInfo(name = "567", startEpochSeconds = 4, endEpochSeconds = -1))
        assertFalse(flow.first())
    }
}