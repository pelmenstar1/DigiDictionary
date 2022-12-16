package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertEquals

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

    @Test
    fun isValidTest() {
        val field0 = ValidityFlow.Field(ordinal = 0)
        val field1 = ValidityFlow.Field(ordinal = 1)
        val field2 = ValidityFlow.Field(ordinal = 2)

        fun testCase(fields: Array<out ValidityFieldEnabledComputed>, expected: Boolean) {
            val flow = createFlow(fields)

            assertEquals(expected, flow.isValid)
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
    fun isValidExceptNoComputedFieldsTest() {
        val field0 = ValidityFlow.Field(ordinal = 0)
        val field1 = ValidityFlow.Field(ordinal = 1)
        val field2 = ValidityFlow.Field(ordinal = 2)

        fun testCase(fields: Array<out ValidityFieldEnabledComputed>, expected: Boolean) {
            val flow = createFlow(fields)

            assertEquals(expected, flow.isValidExceptNotComputedFields)
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
                    field0, isEnabled = false, isComputed = false
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
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = true
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = false, isComputed = true
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false
                ),
                ValidityFieldEnabledComputed(
                    field2, isEnabled = false, isComputed = false
                )
            ),
            expected = true
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = false, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = false
                ),
                ValidityFieldEnabledComputed(
                    field2, isEnabled = false, isComputed = false
                )
            ),
            expected = false
        )

        testCase(
            fields = arrayOf(
                ValidityFieldEnabledComputed(
                    field0, isEnabled = true, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field1, isEnabled = true, isComputed = true
                ),
                ValidityFieldEnabledComputed(
                    field2, isEnabled = true, isComputed = true
                )
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
}