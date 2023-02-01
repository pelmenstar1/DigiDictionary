package io.github.pelmenstar1.digiDict.common

import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.commonTestUtils.assertTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewModelActionTests {
    var isActionCalled = false
    val errorActionException = Exception()

    inner class VM : ViewModel() {
        val action = viewModelAction("TAG") {
            isActionCalled = true
        }

        val errorAction = viewModelAction("TAG") {
            throw errorActionException
        }

        val cancellationAction = viewModelAction("TAG") {
            throw CancellationException()
        }
    }

    @Test
    fun runTest_successFlow() = runTest {
        val vm = VM()

        assertTimeout(timeout = 200) {
            // Before the action is run, the success flow should be empty
            vm.action.successFlow.first()
        }

        vm.action.run()

        // That's all. If there's no value sent to successFlow, the test will timeout.
        vm.action.successFlow.first()
    }

    @Test
    fun runTest_errorFlow_noValueWhenNoError() = runTest {
        val vm = VM()

        assertTimeout(timeout = 200) {
            // Before the action is run, the error flow should be empty
            vm.action.errorFlow.first()
        }

        vm.action.run()

        assertTimeout(timeout = 200) {
            // If there's no error, the error flow should be empty
            vm.action.errorFlow.first()
        }
    }

    @Test
    fun runTest_errorFlow_skipCancellationException() = runTest {
        val vm = VM()

        vm.cancellationAction.run()

        assertTimeout(timeout = 200) {
            vm.cancellationAction.errorFlow.first()
        }
    }

    @Test
    fun runTest_errorFlow() = runTest {
        val vm = VM()

        vm.errorAction.run()

        assertTimeout(timeout = 200) {
            // If there's error, the success flow should be empty
            vm.errorAction.successFlow.first()
        }

        val actualException = vm.errorAction.errorFlow.first()
        assertSame(errorActionException, actualException)
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