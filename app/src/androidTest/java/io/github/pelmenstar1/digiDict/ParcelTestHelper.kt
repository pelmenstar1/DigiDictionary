package io.github.pelmenstar1.digiDict

import android.os.Parcel
import android.os.Parcelable
import kotlin.test.assertEquals

object ParcelTestHelper {
    fun <T : Parcelable> readWriteTest(value: T, creator: Parcelable.Creator<T>) {
        val parcel = Parcel.obtain()
        value.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdFromParcel = creator.createFromParcel(parcel)

        assertEquals(value, createdFromParcel)
    }
}