package io.github.pelmenstar1.digiDict.common

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.commonTestUtils.Diff
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class FilteredArrayDiffTests {
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

    private inline fun diffTestHelperInternal(
        old: Array<DataObject>,
        new: Array<DataObject>,
        diffMethod: (FilteredArray<DataObject>, FilteredArray<DataObject>, FilteredArrayDiffItemCallback<DataObject>) -> FilteredArrayDiffResult
    ) {
        val oldFilteredArray = FilteredArray(old, old.size)
        val newFilteredArray = FilteredArray(new, new.size)

        val diffResult = diffMethod(oldFilteredArray, newFilteredArray, DataObjectFilteredArrayItemCallback)
        val actualRanges = ArrayList<Diff.TypedIntRange>()
        diffResult.dispatchTo(Diff.ListUpdateCallbackToList(actualRanges))

        val diffUtilResult = DiffUtil.calculateDiff(
            DataObjectRecyclerViewCallback(oldFilteredArray, newFilteredArray), false
        )

        val expectedRanges = ArrayList<Diff.TypedIntRange>()
        diffUtilResult.dispatchUpdatesTo(Diff.RecyclerViewListUpdateCallbackToList(expectedRanges))

        assertContentEquals(
            expectedRanges,
            actualRanges,
            "old: ${old.contentToString()}, new: ${new.contentToString()}"
        )
    }


    private fun diffTestCases(diffTestHelper: (old: Array<DataObject>, new: Array<DataObject>) -> Unit) {
        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)),
            arrayOf(DataObject(id = 1), DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 1), DataObject(id = 2)),
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)),
            arrayOf(DataObject(id = 1))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)),
            arrayOf(DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 2)),
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 1)),
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0)),
            arrayOf(DataObject(id = 0))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1)),
            arrayOf(DataObject(id = 0), DataObject(id = 2))
        )

        diffTestHelper(
            emptyArray(),
            arrayOf(DataObject(id = 0))
        )

        diffTestHelper(
            emptyArray(),
            arrayOf(DataObject(id = 0), DataObject(id = 1))
        )

        diffTestHelper(
            emptyArray(),
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0)), emptyArray()
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1)),
            emptyArray()
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2)),
            arrayOf(DataObject(id = 2))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3)),
            arrayOf(DataObject(id = 1), DataObject(id = 3))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 3)),
            arrayOf(DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0, value = 0)),
            arrayOf(DataObject(id = 0, value = 1))
        )

        diffTestHelper(
            arrayOf(DataObject(id = 0), DataObject(id = 1, value = 0)),
            arrayOf(DataObject(id = 1, value = 1))
        )

        diffTestHelper(
            arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            arrayOf(
                DataObject(id = 0, value = 1),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
                DataObject(id = 4, value = 1)
            )
        )

        diffTestHelper(
            arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            arrayOf(
                DataObject(id = 0, value = 1),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
                DataObject(id = 4)
            )
        )

        diffTestHelper(
            arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3), DataObject(id = 4)
            ),
            arrayOf(
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3),
            )
        )

        diffTestHelper(
            arrayOf(
                DataObject(id = 0), DataObject(id = 1), DataObject(id = 2), DataObject(id = 3)
            ),
            arrayOf(
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3, value = 1),
                DataObject(id = 4)
            )
        )

        diffTestHelper(
            arrayOf(
                DataObject(id = 1)
            ),
            arrayOf(
                DataObject(id = 0),
                DataObject(id = 1, value = 1),
                DataObject(id = 2),
                DataObject(id = 3, value = 1),
                DataObject(id = 4)
            )
        )
    }

    private fun diffTestHelperShort(old: Array<DataObject>, new: Array<DataObject>) {
        val impl = FilteredArrayDiffManagerDelegateShortImpl<DataObject>()

        diffTestHelperInternal(old, new, impl::calculateDifference)
    }

    private fun diffTestHelperLong(old: Array<DataObject>, new: Array<DataObject>) {
        val impl = FilteredArrayDiffManagerDelegateLongImpl<DataObject>()

        diffTestHelperInternal(old, new, impl::calculateDifference)
    }

    @Test
    fun diffTest_short_onDistinctManagers() {
        diffTestCases(::diffTestHelperShort)
    }

    @Test
    fun diffTest_short_onSameManager() {
        val impl = FilteredArrayDiffManagerDelegateShortImpl<DataObject>()

        diffTestCases { old, new ->
            diffTestHelperInternal(old, new, impl::calculateDifference)
        }
    }

    @Test
    fun diffTest_long_onDistinctManagers() {
        diffTestCases(::diffTestHelperLong)
    }

    @Test
    fun diffTest_long_onSameManager() {
        val impl = FilteredArrayDiffManagerDelegateLongImpl<DataObject>()

        diffTestCases { old, new ->
            diffTestHelperInternal(old, new, impl::calculateDifference)
        }
    }

    private fun diffRandomizedTestInternal(diffTestHelper: (old: Array<DataObject>, new: Array<DataObject>) -> Unit) {
        val random = Random(2023)

        for (arraySize in intArrayOf(20, 50, 100)) {
            val oldArray = Array(arraySize) { DataObject(id = it) }

            repeat(500) {
                val newArray = Array(arraySize) { DataObject(id = it) }.also {
                    it.shuffle(random)
                }

                diffTestHelper(oldArray, newArray)
            }
        }
    }

    @Test
    fun diffRandomizedTest_short_onDistinctManagers() {
        diffRandomizedTestInternal(::diffTestHelperShort)
    }

    @Test
    fun diffRandomizedTest_short_onSameManager() {
        val manager = FilteredArrayDiffManagerDelegateShortImpl<DataObject>()

        diffRandomizedTestInternal { old, new ->
            diffTestHelperInternal(old, new, manager::calculateDifference)
        }
    }

    @Test
    fun diffRandomizedTest_long_onDistinctManagers() {
        diffRandomizedTestInternal(::diffTestHelperLong)
    }

    @Test
    fun diffRandomizedTest_long_onSameManager() {
        val manager = FilteredArrayDiffManagerDelegateLongImpl<DataObject>()

        diffRandomizedTestInternal { old, new ->
            diffTestHelperInternal(old, new, manager::calculateDifference)
        }
    }

    @Test
    fun diffManager_delegateSwitchedToLongImplOnFirstRequest() {
        val manager = FilteredArrayDiffManager(DataObjectFilteredArrayItemCallback)

        manager.resolveDelegate(oldSize = 0xFFFF, newSize = 0xFFFF)
        assertTrue(manager.delegate is FilteredArrayDiffManagerDelegateLongImpl)
    }

    @Test
    fun diffManager_delegateSwitchTest() {
        val manager = FilteredArrayDiffManager(DataObjectFilteredArrayItemCallback)

        manager.resolveDelegate(oldSize = 1, newSize = 1)
        assertTrue(manager.delegate is FilteredArrayDiffManagerDelegateShortImpl)

        manager.resolveDelegate(oldSize = 2, newSize = 0xFFFF)
        assertTrue(manager.delegate is FilteredArrayDiffManagerDelegateLongImpl)

        // The manager shouldn't switch to short-impl once it switched to long-impl
        manager.resolveDelegate(oldSize = 1, newSize = 1)
        assertTrue(manager.delegate is FilteredArrayDiffManagerDelegateLongImpl)
    }
}