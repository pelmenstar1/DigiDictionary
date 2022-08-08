package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.utils.trimToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalSearchQueryProvider {
    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow = _isActiveFlow.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    val queryFlow = _queryFlow.asStateFlow()

    var isActive: Boolean
        get() = _isActiveFlow.value
        set(value) {
            _isActiveFlow.value = value
        }

    var query: CharSequence
        get() = _queryFlow.value
        set(value) {
            _queryFlow.value = value.trimToString()
        }
}