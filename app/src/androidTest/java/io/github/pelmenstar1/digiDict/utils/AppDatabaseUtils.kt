package io.github.pelmenstar1.digiDict.utils

import android.content.Context
import io.github.pelmenstar1.digiDict.data.AppDatabase

object AppDatabaseUtils {
    fun createTestDatabase(context: Context): AppDatabase {
        return AppDatabase.createInMemory(context)
    }
}