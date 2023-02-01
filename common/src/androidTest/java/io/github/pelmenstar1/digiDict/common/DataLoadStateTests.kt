package io.github.pelmenstar1.digiDict.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class DataLoadStateTests {
    private data class TestObject(val number: Int)

    private fun createDataStateFlowWithoutRetry(
        block: DataLoadStateManager.FlowBuilder<TestObject>.() -> Flow<DataLoadState<TestObject>>
    ): Flow<DataLoadState<TestObject>> {
        return DataLoadStateManager.FlowBuilder(DataLoadStateManager<TestObject>("TAG")).block()
    }

    private fun assertSuccessState(state: DataLoadState<TestObject>, expectedValue: TestObject) {
        assertIs<DataLoadState.Success<TestObject>>(state)
        assertSame(expectedValue, state.value)
    }

    private suspend fun loadStateSequenceTestHelper(
        flow: Flow<DataLoadState<TestObject>>,
        vararg expectedValues: TestObject
    ) {
        val states = flow.toList()

        assertEquals(expectedValues.size + 1, states.size)
        assertEquals(DataLoadState.loading(), states[0])

        for (i in expectedValues.indices) {
            val state = states[i + 1]
            val expectedValue = expectedValues[i]

            assertSuccessState(state, expectedValue)
        }
    }

    @Test
    fun loadStateSequenceValidityTest_success_action() = runTest {
        val testObject = TestObject(1)
        val flow = createDataStateFlowWithoutRetry {
            fromAction { testObject }
        }

        loadStateSequenceTestHelper(flow, testObject)
    }

    @Test
    fun loadStateSequenceValidityTest_success_flow() = runTest {
        val testObjects = (1..3).map { TestObject(it) }.toTypedArray()

        val flow = createDataStateFlowWithoutRetry {
            fromFlow(flowOf(*testObjects))
        }

        loadStateSequenceTestHelper(flow, *testObjects)
    }

    @Test
    fun loadStateSequenceValidityTest_error_action() = runTest {
        val flow = createDataStateFlowWithoutRetry {
            fromAction { throw Exception() }
        }

        val states = flow.toList()

        assertEquals(2, states.size)
        assertEquals(DataLoadState.loading(), states[0])
        assertEquals(DataLoadState.error(), states[1])
    }

    @Test
    fun loadStateSequenceValidityTest_error_flow() = runTest {
        val values = arrayOf(TestObject(1), TestObject(2))

        val flow = createDataStateFlowWithoutRetry {
            fromFlow {
                flow {
                    values.forEach { emit(it) }
                    throw Exception()
                }
            }
        }

        val states = flow.toList()

        assertEquals(values.size + 2, states.size)
        assertEquals(DataLoadState.loading(), states[0])

        for (i in values.indices) {
            val state = states[i + 1]
            val value = values[i]

            assertSuccessState(state, value)
        }

        assertEquals(DataLoadState.error(), states[values.size + 1])
    }

    @Test
    fun retryTest_thenSuccess_noError_action() = runTest {
        var callCount = 0
        val values = (1..2).map { TestObject(it) }.toTypedArray()

        val manager = DataLoadStateManager<TestObject>("TAG")
        val flow = manager.buildFlow(this) {
            fromAction { values[callCount++] }
        }

        val stateSequence1 = flow.take(2).toList()

        assertEquals(DataLoadState.loading(), stateSequence1[0])
        assertSuccessState(stateSequence1[1], expectedValue = values[0])

        manager.retrySuspend()

        val stateSequence2 = flow.take(2).toList()

        assertEquals(DataLoadState.loading(), stateSequence2[0])
        assertSuccessState(stateSequence2[1], expectedValue = values[1])
    }
}