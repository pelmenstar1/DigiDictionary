package io.github.pelmenstar1.digiDict.utils

import android.os.Bundle
import android.os.Parcel

fun Parcel.readStringOrThrow() = requireNotNull(readString())
fun Bundle.getIntArrayOrThrow(key: String) = requireNotNull(getIntArray(key))