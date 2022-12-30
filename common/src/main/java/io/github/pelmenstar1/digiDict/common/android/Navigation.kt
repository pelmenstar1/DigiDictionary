package io.github.pelmenstar1.digiDict.common.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import io.github.pelmenstar1.digiDict.common.launchFlowCollector

fun LifecycleOwner.popBackStackOnSuccess(action: ViewModelAction, navController: NavController) {
    lifecycleScope.launchFlowCollector(action.successFlow) {
        navController.popBackStack()
    }
}