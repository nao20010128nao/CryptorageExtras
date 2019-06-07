@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.ConcatenatedInputStream
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.InputStream

fun FileSource.withNamePrefixed(name: String): FileSource = PrefixedByFileSource(this, name)

fun FileSource.withSplitFilesCombined(): FileSource = SplitFilesCombinedFileSource(this)


internal val splitFilename: Regex = Regex("(.+)\\.[0-9]{3}\\.split$")
internal val splitZeroFilename: Regex = Regex("(.+)\\.000\\.split$")

internal inline fun makeDedicatedSplitFilename(name: String): Regex = Regex(Regex.escape(name) + "\\.[0-9]{3}\\.split$")

internal inline fun Iterator<InputStream>.combined(): InputStream = ConcatenatedInputStream(this)

internal inline fun InputStream.skip(length: Int): InputStream = also {
    ByteStreams.skipFully(this, length.toLong())
}

fun Cryptorage.logged(tag: String? = null): Cryptorage {
    return object : Cryptorage by this {
        inline fun <T> w(key: String, name: String, aa: () -> T): T {
            val response = aa()
            println("$tag: $key: $name: $response")
            return response
        }

        override fun lastModified(name: String): Long = w("lastModified", name) { this@logged.lastModified(name) }

        override fun open(name: String, offset: Int): ByteSource = w("open", name) { this@logged.open(name, offset) }

        override fun put(name: String): ByteSink = w("put", name) { this@logged.put(name) }

        override fun size(name: String): Long = w("size", name) { this@logged.size(name) }
    }
}
