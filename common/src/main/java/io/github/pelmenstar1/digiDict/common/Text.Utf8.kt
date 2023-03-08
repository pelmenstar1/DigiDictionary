package io.github.pelmenstar1.digiDict.common

// Basically, this is Square Okio Utf8.kt with some minor changes.
// https://github.com/square/okio/blob/master/okio/src/commonMain/kotlin/okio/Utf8.kt

internal const val REPLACEMENT_BYTE: Byte = '?'.code.toByte()

internal fun String.utf8Size(): Int {
    val length = length
    var result = 0

    var i = 0
    while (i < length) {
        val c = this[i].code

        if (c < 0x80) {
            // A 7-bit character with 1 byte.
            result++
            i++
        } else if (c < 0x800) {
            // An 11-bit character with 2 bytes.
            result += 2
            i++
        } else if (c < 0xd800 || c > 0xdfff) {
            // A 16-bit character with 3 bytes.
            result += 3
            i++
        } else {
            val low = if (i + 1 < length) this[i + 1].code else 0

            if (c > 0xdbff || low < 0xdc00 || low > 0xdfff) {
                // A malformed surrogate, which yields '?'.
                result++
                i++
            } else {
                // A 21-bit character with 4 bytes.
                result += 4
                i += 2
            }
        }
    }

    return result
}

internal inline fun String.processUtf8Bytes(
    on1Byte: (Byte) -> Unit,
    on2Byte: (Byte, Byte) -> Unit,
    on3Byte: (Byte, Byte, Byte) -> Unit,
    on4Byte: (Byte, Byte, Byte, Byte) -> Unit
) {
    // Transcode a UTF-16 String to UTF-8 bytes.
    var index = 0
    val length = length

    while (index < length) {
        val c = this[index]

        when {
            c < '\u0080' -> {
                // Emit a 7-bit character with 1 byte.
                on1Byte(c.code.toByte()) // 0xxxxxxx
                index++

                // Assume there is going to be more ASCII
                while (index < length && this[index] < '\u0080') {
                    on1Byte(this[index++].code.toByte())
                }
            }

            c < '\u0800' -> {
                // Emit a 11-bit character with 2 bytes.
                on2Byte(
                    (c.code shr 6 or 0xc0).toByte(), // 110xxxxx
                    (c.code and 0x3f or 0x80).toByte() // 10xxxxxx
                )

                index++
            }

            c !in '\ud800'..'\udfff' -> {
                // Emit a 16-bit character with 3 bytes.
                on3Byte(
                    (c.code shr 12 or 0xe0).toByte(), // 1110xxxx
                    (c.code shr 6 and 0x3f or 0x80).toByte(), // 10xxxxxx
                    (c.code and 0x3f or 0x80).toByte() // 10xxxxxx
                )

                index++
            }

            else -> {
                // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                // byte.

                if (c > '\udbff' || length <= index + 1 || this[index + 1] !in '\udc00'..'\udfff') {
                    on1Byte(REPLACEMENT_BYTE)
                    index++
                } else {
                    // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                    // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                    // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                    val codePoint = (((c.code shl 10) + this[index + 1].code) + (0x010000 - (0xd800 shl 10) - 0xdc00))

                    // Emit a 21-bit character with 4 bytes.
                    on4Byte(
                        (codePoint shr 18 or 0xf0).toByte(), // 11110xxx
                        (codePoint shr 12 and 0x3f or 0x80).toByte(), // 10xxxxxx
                        (codePoint shr 6 and 0x3f or 0x80).toByte(), // 10xxyyyy
                        (codePoint and 0x3f or 0x80).toByte() // 10yyyyyy
                    )

                    index += 2
                }
            }
        }
    }
}