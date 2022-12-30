package io.github.pelmenstar1.digiDict.common.android

import android.os.Bundle
import android.os.Parcel

fun Parcel.readStringOrThrow() = requireNotNull(readString())
fun Bundle.getByteArrayOrThrow(key: String) = requireNotNull(getByteArray(key))