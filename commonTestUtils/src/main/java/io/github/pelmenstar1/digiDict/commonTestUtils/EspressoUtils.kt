package io.github.pelmenstar1.digiDict.commonTestUtils

import android.graphics.PointF
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap

fun clickAt(point: PointF): GeneralClickAction {
    return clickAt(point.x, point.y)
}

fun clickAt(x: Float, y: Float): GeneralClickAction {
    return GeneralClickAction(
        Tap.SINGLE,
        { view ->
            val screenPos = IntArray(2)
            view.getLocationOnScreen(screenPos)

            val screenX = screenPos[0] + x
            val screenY = screenPos[1] + y

            floatArrayOf(screenX, screenY)
        },
        Press.FINGER,
        InputDevice.SOURCE_UNKNOWN,
        MotionEvent.BUTTON_PRIMARY
    )
}