package io.github.pelmenstar1.digiDict.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.serialization.BinarySerializer
import io.github.pelmenstar1.digiDict.common.serialization.BinarySerializerResolverBuilder
import io.github.pelmenstar1.digiDict.common.serialization.ValueReader
import io.github.pelmenstar1.digiDict.common.serialization.ValueWriter
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MutliVersionBinarySerializerResolverTests {
    private fun fakeBinarySerializer() = object : BinarySerializer<Any> {
        override fun newArrayOfNulls(size: Int) = arrayOfNulls<Any>(size)
        override fun getByteSize(value: Any) = 0
        override fun readFrom(reader: ValueReader) = Any()

        override fun writeTo(writer: ValueWriter, value: Any) {
        }
    }

    @Test
    fun getTest() {
        fun testCase(vararg versions: Int) {
            val builder = BinarySerializerResolverBuilder<Any>()
            val serializers = versions.map { fakeBinarySerializer() }

            for (i in versions.indices) {
                val version = versions[i]
                val serializer = serializers[i]

                builder.forVersion(version, serializer)
            }

            val resolver = builder.build()
            for (i in versions.indices) {
                val version = versions[i]
                val expectedSerializer = serializers[i]
                val actualSerializer = resolver.getOrLatest(version)

                assertEquals(expectedSerializer, actualSerializer)
            }
        }

        testCase(0)
        testCase(1, 2)
        testCase(1, 2, 3, 4, 5, 7, 10)
    }

    @Test
    fun getLatestTest() {
        fun testCase(versions: IntArray, lastVersion: Int, versionToGet: Int) {
            val builder = BinarySerializerResolverBuilder<Any>()
            val serializers = versions.map { fakeBinarySerializer() }

            for (i in versions.indices) {
                val version = versions[i]
                val serializer = serializers[i]

                builder.forVersion(version, serializer)
            }

            val resolver = builder.build()
            val expectedSerializer = serializers[versions.indexOf(lastVersion)]
            val actualSerializer = resolver.getOrLatest(versionToGet)

            assertEquals(expectedSerializer, actualSerializer)
        }

        testCase(
            versions = intArrayOf(0, 1, 2),
            lastVersion = 2,
            versionToGet = 5
        )

        testCase(
            versions = intArrayOf(0),
            lastVersion = 0,
            versionToGet = 1
        )

        testCase(
            versions = intArrayOf(0, 5, 7, 9),
            lastVersion = 9,
            versionToGet = 100
        )
    }
}