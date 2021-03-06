@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.ConcatenatedInputStream
import org.apache.ftpserver.ftplet.FileSystemView
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

fun FileSource.withNamePrefixed(name: String): PrefixedByFileSource = PrefixedByFileSource(this, name)
fun FileSource.withSplitFilesCombined(): SplitFilesCombinedFileSource = SplitFilesCombinedFileSource(this)
fun FileSource.withRetries(retry: Int = 5): RetriesFileSource = RetriesFileSource(this, retry)
fun List<FileSource>.combined(): CombinedFileSource = CombinedFileSource(this)


internal val splitFilename: Regex = Regex("^(.+)\\.[0-9]+\\.split$")
internal val splitZeroFilename: Regex = Regex("~(.+)\\.0+\\.split$")

internal inline fun makeDedicatedSplitFilename(name: String): Regex = Regex(Regex.escape(name) + "\\.([0-9]+)\\.split$")

internal inline fun Iterator<InputStream>.combined(): InputStream = ConcatenatedInputStream(this)

internal inline fun <T> probable(retry: Int = 5, f: () -> T?): T? {
    var lastError: Throwable? = null
    for (i in (1..retry)) {
        return try {
            f()
        } catch (e: Throwable) {
            lastError = e
            null
        } ?: continue
    }
    lastError?.printStackTrace()
    return null
}

internal inline fun <T> wish(f: () -> T?): T? = try {
    f()
} catch (e: Throwable) {
    null
}

fun ByteSource.encrypt(keys: AesKeys): ByteSource = AesEncryptorByteSource(this, keys)

fun testURL(url: String): Boolean = try {
    URL(url)
    true
} catch (e: Throwable) {
    false
}

fun testPath(url: String): Boolean = try {
    File(url)
    true
} catch (e: Throwable) {
    false
}

fun Cryptorage.logged(tag: String? = null): Cryptorage = object : Cryptorage by this {
    inline fun <T> w(key: String, name: String, aa: () -> T): T {
        val response = aa()
        println("$tag: $key: $name: $response")
        return response
    }

    override fun lastModified(name: String): Long = w("lastModified", name) { this@logged.lastModified(name) }
    override fun open(name: String, offset: Int): ByteSource = w("open", name) { this@logged.open(name, offset) }
    override fun open(name: String): ByteSource = w("open", name) { this@logged.open(name, 0) }
    override fun put(name: String): ByteSink = w("put", name) { this@logged.put(name) }
    override fun size(name: String): Long = w("size", name) { this@logged.size(name) }
}

fun FileSource.logged(tag: String? = null): FileSource = object : FileSource by this {
    inline fun <T> w(key: String, name: String, aa: () -> T): T {
        val response = aa()
        println("$tag: $key: $name: $response")
        return response
    }

    override fun lastModified(name: String): Long = w("lastModified", name) { this@logged.lastModified(name) }
    override fun open(name: String, offset: Int): ByteSource = w("open", name) { this@logged.open(name, offset) }
    override fun open(name: String): ByteSource = w("open", name) { this@logged.open(name, 0) }
    override fun put(name: String): ByteSink = w("put", name) { this@logged.put(name) }
    override fun size(name: String): Long = w("size", name) { this@logged.size(name) }
}

fun Cryptorage.forFtpServer(): FileSystemView = CryptorageFileSystemView(this)

fun <T> Sequence<T?>.nonNulls(): Sequence<T> = filter { it != null }.map { it!! }

fun source(f: () -> InputStream): ByteSource = object : ByteSource() {
    override fun openStream(): InputStream = f()
}

fun sink(f: () -> OutputStream): ByteSink = object : ByteSink() {
    override fun openStream(): OutputStream = f()
}
