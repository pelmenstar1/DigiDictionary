package io.github.pelmenstar1.digiDict.utils

import androidx.navigation.NavController

fun NavController.popBackStackLambda(): () -> Unit {
    return { popBackStack() }
}