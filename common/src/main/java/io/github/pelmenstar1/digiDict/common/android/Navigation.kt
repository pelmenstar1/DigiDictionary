package io.github.pelmenstar1.digiDict.common.android

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

fun LifecycleOwner.popBackStackOnSuccess(action: ViewModelAction<*>, navController: NavController) {
    launchFlowCollector(action.successFlow) {
        navController.popBackStack()
    }
}