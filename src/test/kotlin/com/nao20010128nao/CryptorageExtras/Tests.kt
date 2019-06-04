package com.nao20010128nao.CryptorageExtras

import com.nao20010128nao.Cryptorage.combine
import com.nao20010128nao.Cryptorage.newMemoryFileSource
import org.junit.Test

class Tests {
    @Test
    fun testPrefixWork() {
        val mem = mapOf(
                "a-a" to zeroBytes,
                "a-b" to zeroBytes,
                "a-c" to zeroBytes,
                "a-d" to zeroBytes,
                "b-d" to zeroBytes
        ).newMemoryFileSource()
        val prefix = mem.withNamePrefixed("a-")
        require(prefix.list().contentEquals(arrayOf("a", "b", "c", "d")))
    }

    @Test
    fun testSplitsName() {
        val mem = mapOf(
                "a.000.split" to zeroBytes,
                "a.001.split" to zeroBytes,
                "a.002.split" to zeroBytes,
                "a.003.split" to zeroBytes,
                "b" to zeroBytes
        ).newMemoryFileSource()
        val prefix = mem.withSplitFilesCombined()
        require(prefix.list().contentEquals(arrayOf("a", "b")))
    }

    @Test
    fun testSplitsCombined() {
        val mem = mapOf(
                "a.000.split" to byteArrayOf(1),
                "a.001.split" to byteArrayOf(2),
                "a.002.split" to byteArrayOf(3),
                "a.003.split" to byteArrayOf(4)
        ).newMemoryFileSource()
        val prefix = mem.withSplitFilesCombined()
        require(byteArrayOf(1, 2, 3, 4).contentEquals(prefix.open("a").read()))
    }

    @Test
    fun testSplitsCombinedWithSkips() {
        val mem = mapOf(
                "a.000.split" to byteArrayOf(1),
                "a.005.split" to byteArrayOf(2),
                "a.010.split" to byteArrayOf(3),
                "a.020.split" to byteArrayOf(4)
        ).newMemoryFileSource()
        val prefix = mem.withSplitFilesCombined()
        require(byteArrayOf(1, 2, 3, 4).contentEquals(prefix.open("a").read()))
    }

    @Test
    fun testSplitsCombinedSize() {
        val mem = mapOf(
                "a.000.split" to byteArrayOf(1),
                "a.001.split" to byteArrayOf(2),
                "a.002.split" to byteArrayOf(3),
                "a.003.split" to byteArrayOf(4),
                "a.005.split" to byteArrayOf(2),
                "a.010.split" to byteArrayOf(3),
                "a.020.split" to byteArrayOf(4)
        ).newMemoryFileSource()
        val prefix = mem.withSplitFilesCombined()
        require(prefix.size("a") == 7L)
    }

    @Test
    fun testSplitsCombinedSizeComplicated() {
        val mem = listOf(
                "a.000.split" to byteArrayOf(1),
                "a.001.split" to byteArrayOf(2),
                "a.002.split" to byteArrayOf(3),
                "a.003.split" to byteArrayOf(4),
                "a.005.split" to byteArrayOf(2),
                "a.010.split" to byteArrayOf(3),
                "a.020.split" to byteArrayOf(4)
        ).map { mapOf(it).newMemoryFileSource().fakeWrap() }.combine().logged()

        val prefix = mem.withSplitFilesCombined()
        require(prefix.size("a") == 7L)
    }
}
