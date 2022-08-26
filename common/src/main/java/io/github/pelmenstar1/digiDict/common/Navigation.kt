package io.github.pelmenstar1.digiDict.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController

fun LifecycleOwner.popBackStackOnSuccess(action: ViewModelAction, navController: NavController) {
    lifecycleScope.launchFlowCollector(action.successFlow) {
        navController.popBackStack()
    }
}