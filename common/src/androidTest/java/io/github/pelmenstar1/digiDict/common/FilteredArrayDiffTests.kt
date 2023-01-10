package io.github.pelmenstar1.digiDict.common

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.commonTestUtils.FilteredArrayTestUtils
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FilteredArrayDiffTests {
    private enum class RangeType {
        INSERTED,
        REMOVED,
        CHANGED
    }

    private data class TypedIntRange(val type: RangeType, val position: Int, val count: Int) {
        override fun toString(): String {
            return "($type: pos=$position; count=$count)"
        }
    }

    /*
    private class RangeEmitter {
        val ranges = ArrayList<TypedIntRange>()

        fun inserted(position: Int, count: Int) = addRange(RangeType.INSERTED, position, count)
        fun removed(position: Int, count: Int) = addRange(RangeType.REMOVED, position, count)
        fun changed(position: Int, count: Int) = addRange(RangeType.CHANGED, position, count)

        private fun addRange(type: RangeType, position: Int, count: Int) {
            ranges.add(TypedIntRange(type, position, count))
        }
    }
    */

    private data class DataObject(val id: Int, val value: Int = 0)

    private object DataObjectFilteredArrayItemCallback : FilteredArrayDiffItemCallback<DataObject> {
        override fun areItemsTheSame(a: DataObject, b: DataObject): Boolean {
            return a.id == b.id
        }

        override fun areContentsTheSame(a: DataObject, b: DataObject): Boolean {
            return a == b
        }
    }

    private class DataObjectRecyclerViewCallback(
        val oldArray: FilteredArray<DataObject>,
        val newArray: FilteredArray<DataObject>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldArray.size
        override fun getNewListSize() = newArray.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldArray[oldItemPosition].id == newArray[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldArray[oldItemPosition] == newArray[newItemPosition]
        }
    }

    private fun diffTestHelper(
        old: Array<DataObject>,
        oldPassedIndices: IntArray? = null,
        new: Array<DataObject>,
        newPassedIndices: IntArray? = null
    ) {
        val oldFilteredArray = FilteredArray.createUnsafe(
            old, FilteredArrayTestUtils.createBitSet(old.size, oldPassedIndices)
        )

        val newFilteredArray = FilteredArray.createUnsafe(
            new, FilteredArrayTestUtils.createBitSet(new.size, newPassedIndices)
        )

        val diffResult = oldFilteredArray.calculateDifference(newFilteredArray, DataObjectFilteredArrayItemCallback)
        val actualRanges = ArrayList<TypedIntRange>()

        diffResult.dispatchTo(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) = onEvent(RangeType.INSERTED, position, count)
            override fun onRemoved(position: Int, count: Int) = onEvent(RangeType.REMOVED, position, count)
            override fun onChanged(position: Int, count: Int) = onEvent(RangeType.CHANGED, position, count)

            private fun onEvent(type: RangeType, position: Int, count: Int) {
                actualRanges.add(TypedIntRange(type, position, count))
            }
        })

        val diffUtilResult =
            DiffUtil.calculateDiff(DataObjectRecyclerViewCallback(oldFilteredArray, newFilteredArray), false)
        val diffUtilRanges = ArrayList<TypedIntRange>()

        diffUtilResult.dispatchUpdatesTo(object : androidx.recyclerview.widget.ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) = onEvent(RangeType.INSERTED, position, count)
            override fun onRemoved(position: Int, count: Int) = onEvent(RangeType.REMOVED, position, count)
            override fun onChanged(position: Int, count: Int, payload: Any?) =
                onEvent(RangeType.CHANGED, position, count)

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                throw IllegalStateException("Move detection must be disabled")
            }

            private fun onEvent(type: RangeType, position: Int, count: Int) {
                diffUtilRanges.add(TypedIntRange(type, position, count))
            }
        })

        assertContentEquals(diffUtilRanges, actualRanges)
    }

    @Test
    fun diffTest() {
        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 1), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 1), DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 1)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 1)
            ),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0)
            ),
            new = arrayOf(
                DataObject(id = 0)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1)
            ),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = emptyArray(),
            new = arrayOf(
                DataObject(id = 0)
            )
        )

        diffTestHelper(
            old = emptyArray(),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1)
            )
        )

        diffTestHelper(
            old = emptyArray(),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0)
            ),
            new = emptyArray()
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1)
            ),
            new = emptyArray()
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)
            ),
            new = arrayOf(
                DataObject(id = 2)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3)
            ),
            new = arrayOf(
                DataObject(id = 1), DataObject(id = 3)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 3)
            ),
            new = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0, value = 0)
            ),
            new = arrayOf(
                DataObject(id = 0, value = 1)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1, value = 0)
            ),
            new = arrayOf(
                DataObject(id = 1, value = 1)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            new = arrayOf(
                DataObject(id = 0, value = 1),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
                DataObject(id = 4, value = 1)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            new = arrayOf(
                DataObject(id = 0, value = 1),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
                DataObject(id = 4)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            new = arrayOf(
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3)
            ),
            new = arrayOf(
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3, value = 1),
                DataObject(id = 4)
            )
        )

        diffTestHelper(
            old = arrayOf(
                DataObject(id = 1)
            ),
            new = arrayOf(
                DataObject(id = 0),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3, value = 1),
                DataObject(id = 4)
            )
        )
    }

    @Test
    fun diffRandomizedTest() {
        val random = Random(2023)

        for (arraySize in arrayOf(20, 50, 100)) {
            val oldArray = Array(arraySize) { DataObject(id = it) }

            repeat(500) {
                val newArray = Array(arraySize) { DataObject(id = it) }.also {
                    it.shuffle(random)
                }

                diffTestHelper(old = oldArray, new = newArray)
            }
        }
    }
}