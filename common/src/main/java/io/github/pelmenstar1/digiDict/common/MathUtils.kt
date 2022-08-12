package io.github.pelmenstar1.digiDict.common

fun Int.nextPowerOf2(): Int {
    var r = this

    r--
    r = r or (r shr 1)
    r = r or (r shr 2)
    r = r or (r shr 4)
    r = r or (r shr 8)
    r = r or (r shr 16)
    r++

    return r
}