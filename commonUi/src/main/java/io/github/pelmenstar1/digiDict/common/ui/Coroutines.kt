package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.launchFlowCollector
import kotlinx.coroutines.flow.Flow

fun <T : Enum<T>> LifecycleOwner.launchErrorFlowCollector(
    inputLayout: TextInputLayout,
    flow: Flow<T?>,
    formatter: StringFormatter<T>
) {
    launchFlowCollector(flow) { errorType ->
        inputLayout.error = errorType?.let(formatter::format)
    }
}

fun LifecycleOwner.launchSetEnabledFlowCollector(view: View, flow: Flow<Boolean>) {
    launchFlowCollector(flow) { view.isEnabled = it }
}
