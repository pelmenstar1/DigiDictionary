package io.github.pelmenstar1.digiDict.common.binarySerialization

object BinarySerializationConstants {
    const val MAGIC_WORD_1 = 0x00FF00FF_abcdedf00L

    // F0F0F0F0F0F0F0F0
    //
    // A magic word for a second format of binary serialization with version of internal format.
    const val MAGIC_WORD_2 = -1085102592571150096L

    const val LATEST_INTERNAL_FORMAT_VERSION = 1
}