package io.github.pelmenstar1.digiDict.common

import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewModelActionTests {
    var isActionCalled = false

    inner class VM : ViewModel() {
        val action = viewModelAction("TAG") {
            isActionCalled = true
        }
    }

    @Test
    fun runWhenValidTest_fromNotComputedToValidComputed() {
        val vm = VM()
        val field = ValidityFlow.Field(ordinal = 0)
        val flow = ValidityFlow(ValidityFlow.Scheme(field))

        flow.mutate {
            disable(field, isComputed = false)
        }

        isActionCalled = false
        vm.action.runWhenValid(flow)
        assertFalse(isActionCalled)

        Thread.sleep(100)

        flow.mutate {
            enable(field)
        }

        // The action is async, wait some time.
        Thread.sleep(100)

        assertTrue(isActionCalled)
    }

    @Test
    fun runWhenValidTest_fromNotComputedToInvalidComputed() {
        val vm = VM()
        val field = ValidityFlow.Field(ordinal = 0)
        val flow = ValidityFlow(ValidityFlow.Scheme(field))

        flow.mutate {
            disable(field, isComputed = false)
        }

        isActionCalled = false
        vm.action.runWhenValid(flow)
        assertFalse(isActionCalled)

        Thread.sleep(100)

        flow.mutate {
            disable(field)
        }

        // The action is async, wait some time.
        Thread.sleep(100)

        assertFalse(isActionCalled)
    }

    @Test
    fun runWhenValidTest_notComputedShouldNotRun() {
        val vm = VM()
        val field = ValidityFlow.Field(ordinal = 0)
        val flow = ValidityFlow(ValidityFlow.Scheme(field))

        isActionCalled = false
        vm.action.runWhenValid(flow)
        assertFalse(isActionCalled)
    }
}