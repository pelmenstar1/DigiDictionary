package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ValidityFlowTests {
    private class ValidityFieldEnabledComputed(
        val field: ValidityFlow.Field,
        val isEnabled: Boolean,
        val isComputed: Boolean
    )

    private fun createFlow(fields: Array<out ValidityFieldEnabledComputed>): ValidityFlow {
        return ValidityFlow(ValidityFlow.Scheme(fields.mapToArray { it.field })).also { flow ->
            flow.mutate {
                for (f in fields) {
                    set(f.field, f.isEnabled, f.isComputed)
                }
            }
        }
    }

    private fun createFields(fieldCount: Int): Array<ValidityFlow.Field> {
        return Array(fieldCount) { ValidityFlow.Field(ordinal = it) }
    }

    @Test
    fun isValidTest() {
        val field0 = ValidityFlow.Field(ordinal = 0)
        val field1 = ValidityFlow.Field(ordinal = 1)
        val field2 = ValidityFlow.Field(ordinal = 2)

        fun testCase(fields: Array<out ValidityFieldEnabledComputed>, expected: Boolean) {
            val flow = createFlow(fields)

            assertEquals(expected, flow.isAllValid)
        }

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = false
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field1, isEnabled = false, isComputed = false)
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field1, isEnabled = false, isComputed = true)
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field1, isEnabled = false, isComputed = true),
                ValidityFieldEnabledComputed(field2, isEnabled = true, isComputed = true)
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = false),
                ValidityFieldEnabledComputed(field1, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field2, isEnabled = true, isComputed = true)
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = false),
                ValidityFieldEnabledComputed(field1, isEnabled = true, isComputed = false),
                ValidityFieldEnabledComputed(field2, isEnabled = false, isComputed = true)
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(field0, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field1, isEnabled = true, isComputed = true),
                ValidityFieldEnabledComputed(field2, isEnabled = true, isComputed = true)
            ),
            expected = true
        )
    }

    @Test
    fun isAllComputedTest() {
        val field0 = ValidityFlow.Field(ordinal = 0)
        val field1 = ValidityFlow.Field(ordinal = 1)
        val field2 = ValidityFlow.Field(ordinal = 2)

        fun testCase(fields: Array<out ValidityFieldEnabledComputed>, expected: Boolean) {
            val flow = createFlow(fields)

            assertEquals(expected, flow.isAllComputed)
        }

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = false, isComputed = true
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = true,
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = false, isComputed = true,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = false, isComputed = true,
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false,
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = false,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false,
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = false, isComputed = false,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false,
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = false,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false,
                ),
                ValidityFieldEnabledComputed(
                    field2, isEnabled = true, isComputed = false,
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true,
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = true,
                ),
                ValidityFieldEnabledComputed(
                    field2, isEnabled = true, isComputed = true,
                )
            ),
            expected = true
        )
    }

    @Test
    fun waitForAllComputedAndReturnIsAllValidTest() {
        fun testCase(
            fieldCount: Int,
            init: ValidityFlow.Mutator.(Array<ValidityFlow.Field>) -> Unit,
            mutation: ValidityFlow.Mutator.(Array<ValidityFlow.Field>) -> Unit,
            expectedIsValid: Boolean
        ) = runBlocking {
            val fields = createFields(fieldCount)
            val flow = ValidityFlow(ValidityFlow.Scheme(fields))

            flow.mutate { init(fields) }

            launch(Dispatchers.Default) {
                delay(100)
                flow.mutate { mutation(fields) }
            }

            val isValid = flow.waitForAllComputedAndReturnIsAllValid()
            assertEquals(expectedIsValid, isValid)
        }

        testCase(
            fieldCount = 2,
            init = { (field1, field2) ->
                set(field1, value = true)
                set(field2, value = false, isComputed = false)
            },
            mutation = { (_, field2) ->
                set(field2, value = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 2,
            init = { (field1, field2) ->
                set(field1, value = false, isComputed = false)
                set(field2, value = true, isComputed = false)
            },
            mutation = { (field1, field2) ->
                set(field1, value = true)
                set(field2, value = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 2,
            init = { (field1, field2) ->
                set(field1, value = false, isComputed = false)
                set(field2, value = true, isComputed = false)
            },
            mutation = { (field1, field2) ->
                set(field1, value = true)
                set(field2, value = false)
            },
            expectedIsValid = false
        )

        testCase(
            fieldCount = 2,
            init = { (field1, field2) ->
                set(field1, value = false, isComputed = false)
                set(field2, value = true, isComputed = true)
            },
            mutation = { (field1, field2) ->
                set(field1, value = false)
                set(field2, value = false)
            },
            expectedIsValid = false
        )

        testCase(
            fieldCount = 1,
            init = { (field1) ->
                set(field1, value = false, isComputed = false)
            },
            mutation = { (field1) ->
                set(field1, value = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 1,
            init = { (field1) ->
                set(field1, value = false, isComputed = false)
            },
            mutation = { (field1) ->
                set(field1, value = false)
            },
            expectedIsValid = false
        )

        testCase(
            fieldCount = 1,
            init = { (field1) ->
                set(field1, value = true)
            },
            mutation = {},
            expectedIsValid = true
        )
    }

    @Test
    fun waitForComputedAndReturnIsValidTest() {
        fun testCase(
            fieldCount: Int,
            checkFieldIndex: Int,
            init: ValidityFlow.Mutator.(Array<ValidityFlow.Field>) -> Unit,
            mutation: ValidityFlow.Mutator.(Array<ValidityFlow.Field>) -> Unit,
            expectedIsValid: Boolean
        ) = runBlocking {
            val fields = createFields(fieldCount)
            val flow = ValidityFlow(ValidityFlow.Scheme(fields))

            flow.mutate { init(fields) }

            launch(Dispatchers.Default) {
                delay(100)
                flow.mutate { mutation(fields) }
            }

            val isValid = flow.waitForComputedAndReturnIsValid(fields[checkFieldIndex])
            assertEquals(expectedIsValid, isValid)
        }

        testCase(
            fieldCount = 2,
            checkFieldIndex = 1,
            init = { (field1, field2) ->
                set(field1, value = true)
                set(field2, value = false, isComputed = false)
            },
            mutation = { (_, field2) ->
                set(field2, value = true, isComputed = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 2,
            checkFieldIndex = 1,
            init = { (field1, field2) ->
                set(field1, value = true)
                set(field2, value = false, isComputed = false)
            },
            mutation = { (_, field2) ->
                set(field2, value = false, isComputed = true)
            },
            expectedIsValid = false
        )

        testCase(
            fieldCount = 2,
            checkFieldIndex = 1,
            init = { (field1, field2) ->
                set(field1, value = false, isComputed = false)
                set(field2, value = false, isComputed = false)
            },
            mutation = { (_, field2) ->
                set(field2, value = true, isComputed = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 2,
            checkFieldIndex = 0,
            init = { (field1, field2) ->
                set(field1, value = false, isComputed = false)
                set(field2, value = true, isComputed = true)
            },
            mutation = { (field1) ->
                set(field1, value = true, isComputed = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 1,
            checkFieldIndex = 0,
            init = { (field1) ->
                set(field1, value = false, isComputed = false)
            },
            mutation = { (field1) ->
                set(field1, value = true, isComputed = true)
            },
            expectedIsValid = true
        )

        testCase(
            fieldCount = 1,
            checkFieldIndex = 0,
            init = { (field1) ->
                set(field1, value = false, isComputed = false)
            },
            mutation = { (field1) ->
                set(field1, value = false, isComputed = true)
            },
            expectedIsValid = false
        )
    }

    @Test
    fun fieldConstructorThrowsWhenOrdinalOutOfBoundsTest() {
        assertFails {
            ValidityFlow.Field(ordinal = -1)
        }

        assertFails {
            ValidityFlow.Field(ordinal = 16)
        }
    }

    @Test
    fun getTest() {
        fun testCase(fieldCount: Int, fieldIndex: Int, value: Boolean) {
            val fields = createFields(fieldCount)
            val flow = ValidityFlow(ValidityFlow.Scheme(fields))

            flow.mutate {
                set(fields[fieldIndex], value)
            }

            val actualValue = flow[fields[fieldIndex]]
            assertEquals(value, actualValue)
        }

        testCase(fieldCount = 2, fieldIndex = 0, value = true)
        testCase(fieldCount = 2, fieldIndex = 1, value = true)
        testCase(fieldCount = 2, fieldIndex = 0, value = false)
        testCase(fieldCount = 2, fieldIndex = 1, value = false)
        testCase(fieldCount = 1, fieldIndex = 0, value = true)
        testCase(fieldCount = 1, fieldIndex = 0, value = false)
    }
}