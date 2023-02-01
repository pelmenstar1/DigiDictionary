package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.binarySerialization.*
import org.junit.Test
import kotlin.test.assertFails
import kotlin.test.assertSame

class BinarySerializationStaticInfoTests {
    private object Keys : SimpleBinarySerializationSectionKeys<Keys>() {
        val section1 = key<Any>(ordinal = 0, name = "Section 1")
        val section2 = key<Any>(ordinal = 1, name = "Section 2")

        override fun getAll() = arrayOf(section1, section2)
    }

    private val emptySerializer = object : BinarySerializer<Any> {
        override fun newArrayOfNulls(size: Int) = arrayOfNulls<Any>(size)

        override fun writeTo(writer: PrimitiveValueWriter, value: Any) {
        }

        override fun readFrom(reader: PrimitiveValueReader) = Any()
    }


    private fun createEmptyBinaryResolver(): BinarySerializerResolver<Any> = object : BinarySerializerResolver<Any> {
        override val latest: BinarySerializer<Any>
            get() = emptySerializer

        override val latestVersion: Int
            get() = 1

        override fun get(version: Int) = emptySerializer
    }

    @Test
    fun allSectionsShouldBeInitializedTest() {
        assertFails {
            BinarySerializationStaticInfo(Keys) {
                section(Keys.section1, createEmptyBinaryResolver())
            }
        }
    }

    @Test
    fun binarySerializationStaticInfoBuilderTest() {
        val resolver1 = createEmptyBinaryResolver()
        val resolver2 = createEmptyBinaryResolver()

        val staticInfo = BinarySerializationStaticInfo(Keys) {
            section(Keys.section1, resolver1)
            section(Keys.section2, resolver2)
        }

        assertSame(Keys, staticInfo.keys)
        assertSame(resolver1, staticInfo.resolvers[0])
        assertSame(resolver2, staticInfo.resolvers[1])
    }
}