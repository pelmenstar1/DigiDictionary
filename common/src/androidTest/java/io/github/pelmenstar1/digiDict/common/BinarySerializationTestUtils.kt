package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationStaticInfo
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializerResolver
import io.github.pelmenstar1.digiDict.common.binarySerialization.SimpleBinarySerializationSectionKeys

object BinarySerializationTestUtils {
    data class Object1(private val id: Int, private val data: String) {
        companion object {
            val resolver = BinarySerializerResolver<Object1> {
                register<Object1>(
                    version = 1,
                    write = { value ->
                        emit(value.id)
                        emit(value.data)
                    },
                    read = {
                        val id = consumeInt()
                        val data = consumeStringUtf16()

                        Object1(id, data)
                    }
                )
            }
        }
    }

    data class Object2(private val id: Int, private val number: Int, private val data: String) {
        companion object {
            val resolver = BinarySerializerResolver<Object2> {
                register<Object2>(
                    version = 2,
                    write = { value ->
                        emit(value.id)
                        emit(value.number)
                        emit(value.data)
                    },
                    read = {
                        val id = consumeInt()
                        val number = consumeInt()
                        val data = consumeStringUtf16()

                        Object2(id, number, data)
                    }
                )
            }
        }
    }

    object Keys : SimpleBinarySerializationSectionKeys<Keys>() {
        val section1 = key<Object1>(ordinal = 0, "Section 1")
        val section2 = key<Object2>(ordinal = 1, "Section = 2")

        override fun getAll() = arrayOf(section1, section2)
    }

    val staticInfo = BinarySerializationStaticInfo(Keys) {
        section(Keys.section1, Object1.resolver)
        section(Keys.section2, Object2.resolver)
    }
}