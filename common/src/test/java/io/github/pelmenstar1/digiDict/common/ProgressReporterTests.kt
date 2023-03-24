package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ProgressReporterTests {
    @Test
    fun initialProgressTest() {
        assertEquals(ProgressReporter.UNREPORTED, ProgressReporter().progressFlow.value)
    }

    @Test
    fun onProgressTest_percentArg_defaultReporter() {
        fun testCase(value: Int) {
            val reporter = ProgressReporter()
            reporter.onProgress(value)

            assertEquals(value, reporter.progressFlow.value)
        }

        testCase(0)
        testCase(32)
        testCase(100)
    }

    @Test
    fun onProgressTest_percentArg_subReporter() {
        fun testCase(completed: Int, target: Int, value: Int, expectedPercent: Int) {
            val reporter = ProgressReporter().subReporter(completed, target)
            reporter.onProgress(value)

            assertEquals(expectedPercent, reporter.progressFlow.value)
        }

        testCase(completed = 30, target = 50, value = 50, expectedPercent = 40)
        testCase(completed = 10, target = 60, value = 10, expectedPercent = 15)
        testCase(completed = 10, target = 20, value = 1, expectedPercent = 10)
        testCase(completed = 10, target = 50, value = 100, expectedPercent = 50)
        testCase(completed = 10, target = 90, value = 0, expectedPercent = 10)
    }

    @Test
    fun onProgressTest_rangeArgs_defaultReporter() {
        fun testCase(current: Int, total: Int, expected: Int) {
            val reporter = ProgressReporter()
            reporter.onProgress(current, total)

            assertEquals(expected, reporter.progressFlow.value)
        }

        testCase(current = 3, total = 10, expected = 30)
        testCase(current = 1, total = 100, expected = 1)
        testCase(current = 69, total = 100, expected = 69)
        testCase(current = 5, total = 1000, expected = 0)
        testCase(current = 1, total = 2, expected = 50)
        testCase(current = 5, total = 5, expected = 100)
    }

    @Test
    fun onProgressTest_rangeArgs_subReporter() {
        fun testCase(completed: Int, target: Int, current: Int, total: Int, expectedPercent: Int) {
            val reporter = ProgressReporter().subReporter(completed, target)
            reporter.onProgress(current, total)

            assertEquals(expectedPercent, reporter.progressFlow.value)
        }

        testCase(completed = 30, target = 50, current = 2, total = 4, expectedPercent = 40)
        testCase(completed = 10, target = 60, current = 1, total = 10, expectedPercent = 15)
        testCase(completed = 50, target = 60, current = 10, total = 10, expectedPercent = 60)
    }

    @Test
    fun onProgressThrowsTest_singleArg() {
        assertFails { ProgressReporter().onProgress(-1) }
        assertFails { ProgressReporter().onProgress(101) }
    }

    @Test
    fun onProgressThrowsTest_rangeArgs() {
        fun testCase(current: Int, total: Int) {
            val reporter = ProgressReporter()

            assertFails { reporter.onProgress(current, total) }
        }

        testCase(current = -1, total = 5)
        testCase(current = -2, total = -1)
        testCase(current = 5, total = -5)
        testCase(current = 50, total = 40)
    }

    private fun progressOnActionTestHelper(expectedValue: Int, action: ProgressReporter.() -> Unit) {
        val reporter = ProgressReporter()
        reporter.action()

        assertEquals(expectedValue, reporter.progressFlow.value)
    }

    private fun progressOnActionTestHelper(
        completed: Int,
        target: Int,
        expectedValue: Int,
        action: ProgressReporter.() -> Unit
    ) {
        val reporter = ProgressReporter().subReporter(completed, target)
        reporter.action()

        assertEquals(expectedValue, reporter.progressFlow.value)
    }

    @Test
    fun startTest_defaultReporter() = progressOnActionTestHelper(expectedValue = 0) { start() }

    @Test
    fun startTest_subReporter() =
        progressOnActionTestHelper(completed = 30, target = 50, expectedValue = 30) { start() }

    @Test
    fun endTest_defaultReporter() = progressOnActionTestHelper(expectedValue = 100) { end() }

    @Test
    fun endTest_subReporter() = progressOnActionTestHelper(completed = 20, target = 40, expectedValue = 40) { end() }

    @Test
    fun reportErrorTest() = progressOnActionTestHelper(expectedValue = ProgressReporter.ERROR) { reportError() }

    @Test
    fun resetTest() {
        val reporter = ProgressReporter()
        reporter.onProgress(5)
        reporter.reset()

        assertEquals(ProgressReporter.UNREPORTED, reporter.progressFlow.value)
    }

    @Test
    fun subReporterThrowsTest() {
        fun testCase(completed: Int, target: Int) {
            val reporter = ProgressReporter()

            assertFails { reporter.subReporter(completed, target) }
        }

        testCase(completed = -1, target = 5)
        testCase(completed = 101, target = 102)
        testCase(completed = 5, target = 101)
        testCase(completed = 40, target = 30)
    }
}