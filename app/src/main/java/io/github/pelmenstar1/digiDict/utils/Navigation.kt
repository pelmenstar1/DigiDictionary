package io.github.pelmenstar1.digiDict.utils

import androidx.navigation.NavController

fun Event.setPopBackStackHandler(navController: NavController) {
    handler = { navController.popBackStack() }
}