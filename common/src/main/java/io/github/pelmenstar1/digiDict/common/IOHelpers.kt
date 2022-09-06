package io.github.pelmenstar1.digiDict.common

import java.io.IOException
import java.io.InputStream

fun throwNotEnoughDataInStream(): Nothing = throw IOException("Not enough data in the stream")

fun InputStream.readExact(buffer: ByteArray, offset: Int, length: Int) {
    readAtLeast(buffer, offset, minLength = length, maxLength = length)
}

fun InputStream.readAtLeast(buffer: ByteArray, offset: Int, minLength: Int, maxLength: Int): Int {
    when {
        minLength < 0 -> throw IllegalArgumentException("minLength is negative")
        maxLength < 0 -> throw IllegalArgumentException("maxLength is negative")
        minLength > maxLength -> throw IllegalArgumentException("minLength > maxLength")
        maxLength == 0 -> return 0
    }

    var readBytes = 0
    while (readBytes < maxLength) {
        val n = read(buffer, offset + readBytes, maxLength - readBytes)

        if (n <= 0) {
            if (readBytes >= minLength) {
                break
            }

            throwNotEnoughDataInStream()
        }

        readBytes += n
    }

    return readBytes
}