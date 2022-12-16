package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Provides the means to handle validity when there are multiple things that can be either valid or not.
 *
 * In many situations, the validity of particular field is determined in asynchronous manner. It means that when
 * the request to determine validity is sent, the field is set as invalid to prevent the situation of using
 * something when it's not valid. So, we need to know if the field's validity is actually **computed** to false or true,
 * or it's just temporary measure and will be changed to actual value soon. That's the concept of "computed-ness"
 */
class ValidityFlow(val scheme: Scheme) {
    /**
     * Represents a field of the [ValidityFlow]'s scheme.
     *
     * @param ordinal - the ordinal of the field, should be in range [0; 16)
     */
    class Field(val ordinal: Int) {
        init {
            if (ordinal < 0 || ordinal > 15) {
                throw IllegalArgumentException("ordinal can't be negative or greater than 15")
            }
        }

        fun valueMask() = 1 shl (ordinal * 2 + 1)
        fun computedFlagMask() = 1 shl (ordinal * 2)
    }

    /**
     * Represents scheme of [ValidityFlow] that contains the fields and some helpful constants
     *
     * @param fields - ordinals of these fields should be the same as their indices in array
     */
    class Scheme(val fields: Array<out Field>) {
        internal val allValidBits = nBitsSet(fields.size * 2)

        // In binary 1431655765 = 0101 0101 0101 0101 0101 0101 0101 0101,
        // so each bit that denotes whether the field is computed is set
        // Then we AND it with allValidBits in order to get the value for the fields we have.
        internal val allComputedBits = 1431655765 and allValidBits
    }

    /**
     * Provides the means to enable or disable fields. It should be created or used anywhere except [ValidityFlow.mutate]
     */
    class Mutator {
        @PublishedApi
        @JvmField
        internal var bits = 0

        /**
         * Enables given field.
         *
         * @param field a field to enable
         * @param isComputed determines whether the [field] is computed
         */
        fun enable(field: Field, isComputed: Boolean = true) {
            set(field, value = true, isComputed)
        }

        /**
         * Disables given field.
         *
         * @param field a field to disable
         * @param isComputed determines whether the [field] is computed
         */
        fun disable(field: Field, isComputed: Boolean = true) {
            set(field, value = false, isComputed)
        }

        /**
         * Sets the validity of given field.
         *
         * @param field a field to change validity on
         * @param value determines whether the [field] is valid or not
         * @param isComputed determines whether the [field] is computed
         */
        fun set(field: Field, value: Boolean, isComputed: Boolean = true) {
            bits = bits
                .withBit(field.valueMask(), value)
                .withBit(field.computedFlagMask(), isComputed)
        }
    }

    @PublishedApi
    internal val flow = MutableStateFlow(scheme.allComputedBits)

    @PublishedApi
    internal val mutator = Mutator()

    /**
     * Gets whether all the fields are valid.
     */
    val isValid: Boolean
        get() = isValid(flow.value, scheme)

    /**
     * Gets whether all the fields, except not-computed ones, are valid.
     */
    val isValidExceptNotComputedFields: Boolean
        get() = isValidExceptNotComputedFields(flow.value, scheme)

    /**
     * Gets whether all the fields are computed.
     */
    val isAllComputed: Boolean
        get() = isAllComputed(flow.value, scheme)

    /**
     * Gets whether given [field] is valid.
     */
    operator fun get(field: Field) = flow.value and field.valueMask() != 0

    /**
     * Mutates validity of the flow.
     */
    inline fun mutate(block: Mutator.() -> Unit) {
        flow.update {
            mutator.bits = it
            block(mutator)

            mutator.bits
        }
    }

    /**
     * Waits until all the fields are computed.
     */
    suspend fun waitForAllComputed(): Boolean {
        return isAllComputed(flow.first { isAllComputed(it, scheme) }, scheme)
    }

    /**
     * Collects the changes of validity. Note that it will never complete.
     */
    suspend fun collect(collector: FlowCollector<Int>) {
        flow.collect(collector)
    }

    companion object {
        fun isValid(bits: Int, scheme: Scheme): Boolean {
            return bits == scheme.allValidBits
        }

        fun isAllComputed(bits: Int, scheme: Scheme): Boolean {
            val mask = scheme.allComputedBits

            return (bits and mask) == mask
        }

        fun isValidExceptNotComputedFields(bits: Int, scheme: Scheme): Boolean {
            for (f in scheme.fields) {
                // Ignore not-computed fields
                if ((bits and f.computedFlagMask()) != 0) {
                    if ((bits and f.valueMask()) == 0) {
                        return false
                    }
                }
            }

            return true
        }

        // Just to support varargs to make the code more readable
        fun Scheme(vararg fields: Field) = Scheme(fields)
    }
}