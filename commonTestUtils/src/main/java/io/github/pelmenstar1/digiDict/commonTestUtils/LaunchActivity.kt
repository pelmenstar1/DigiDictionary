package io.github.pelmenstar1.digiDict.commonTestUtils

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider

inline fun <reified A : Activity> launchActivity(): ActivityScenario<A> {
    return launchActivity(A::class.java)
}

fun <A : Activity> launchActivity(c: Class<A>): ActivityScenario<A> {
    val compName = ComponentName(ApplicationProvider.getApplicationContext(), c)

    return ActivityScenario.launch(Intent.makeMainActivity(compName))
}