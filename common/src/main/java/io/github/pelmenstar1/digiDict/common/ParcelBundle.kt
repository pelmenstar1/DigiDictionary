package io.github.pelmenstar1.digiDict.common

import android.os.Bundle
import android.os.Parcel

fun Parcel.readStringOrThrow() = requireNotNull(readString())
fun Bundle.getIntArrayOrThrow(key: String) = requireNotNull(getIntArray(key))