package io.github.pelmenstar1.digiDict.common

import androidx.navigation.NavController

fun NavController.popBackStackEventHandler(): () -> Unit = { popBackStack() }