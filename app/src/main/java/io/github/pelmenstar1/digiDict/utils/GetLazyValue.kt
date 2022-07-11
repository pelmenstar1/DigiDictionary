package io.github.pelmenstar1.digiDict.utils

/**
 * If [currentValue] is null,
 * then calls [create] lambda and the returned value of [create] is passed to [set] lambda
 * which should set argument [T] to some member in a class.
 * If [currentValue] isn't null, returns [currentValue]
 *
 * In other words, this just a convenience method for such code:
 * ```
 * class SomeClass {
 *     private var value: SomeType? = null
 *
 *     fun useValue() {
 *         var tempValue = value
 *
 *         if(tempValue == null) {
 *             tempValue = createValue()
 *             value = tempValue
 *         }
 *     }
 * }
 * ```
 *
 * This can be rewritten to:
 * class SomeClass {
 *     private var value: SomeType? = null
 *
 *     fun useValue() {
 *         val tempValue = getLazyValue(
 *             value,
 *             { createValue() },
 *             { value = it }
 *         )
 *     }
 * }
 *
 */
inline fun <T : Any> getLazyValue(currentValue: T?, create: () -> T, set: (T) -> Unit): T {
    currentValue?.let { return it }

    val newValue = create()
    set(newValue)

    return newValue
}