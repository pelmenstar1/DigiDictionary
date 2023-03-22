package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

fun <T : Enum<T>> CoroutineScope.launchErrorFlowCollector(
    inputLayout: TextInputLayout,
    flow: Flow<T?>,
    formatter: StringFormatter<T>
) {
    launchFlowCollector(flow) { errorType ->
        inputLayout.error = errorType?.let(formatter::format)
    }
}

fun CoroutineScope.launchSetEnabledFlowCollector(view: View, flow: Flow<Boolean>) {
    launchFlowCollector(flow) { view.isEnabled = it }
}