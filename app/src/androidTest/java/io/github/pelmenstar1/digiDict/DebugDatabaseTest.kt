package io.github.pelmenstar1.digiDict

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.time.SECONDS_IN_HOUR
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugDatabaseTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.databaseBuilder(appContext, AppDatabase::class.java, "database").build()

    @Test
    fun generateCommon() {
        generateInternal { i, epochSeconds ->
            val ordinal = i + 1

            Record(
                id = 0,
                "Expression$ordinal",
                ComplexMeaning.Common("Meaning$ordinal").rawText,
                "Notes$ordinal",
                score = 0,
                epochSeconds = epochSeconds
            )
        }
    }

    @Test
    fun generateList() {
        generateInternal { i, epochSeconds ->
            val ordinal = i + 1

            Record(
                id = 0,
                "Expression$ordinal",
                ComplexMeaning.List(
                    arrayOf("Meaning${ordinal}_1", "Meaning${ordinal}_2", "Meaning${ordinal}_3")
                ).rawText,
                "Notes$ordinal",
                score = 0,
                epochSeconds = epochSeconds
            )
        }
    }

    private fun generateInternal(createRecord: (index: Int, epochSeconds: Long) -> Record) {
        db.clearAllTables()
        val recordDao = db.recordDao()

        runBlocking {
            var epochSeconds = System.currentTimeMillis() / 1000  - SECONDS_IN_DAY * 10

            repeat(100) { i ->
                recordDao.insert(createRecord(i, epochSeconds))

                epochSeconds += SECONDS_IN_HOUR
            }
        }
    }
}