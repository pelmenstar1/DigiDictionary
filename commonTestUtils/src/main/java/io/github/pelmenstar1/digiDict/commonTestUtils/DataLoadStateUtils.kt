package io.github.pelmenstar1.digiDict.commonTestUtils

import io.github.pelmenstar1.digiDict.common.DataLoadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun <T> Flow<DataLoadState<T>>.waitUntilSuccessOrThrowOnError(): T {
    return filter {
        it is DataLoadState.Success || it is DataLoadState.Error
    }.map {
        if (it is DataLoadState.Error) {
            throw Exception("Error happened in data load state flow")
        }

        (it as DataLoadState.Success).value
    }.first()
}