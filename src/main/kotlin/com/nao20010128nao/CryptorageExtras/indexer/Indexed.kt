@file:Suppress("unused")

package com.nao20010128nao.CryptorageExtras.indexer

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.*
import com.nao20010128nao.CryptorageExtras.*
import com.nao20010128nao.CryptorageExtras.indexer.V1Indexer.Companion.MANIFEST
import com.nao20010128nao.CryptorageExtras.indexer.V1Indexer.Companion.MANIFEST_INDEX
import java.io.File
import java.io.FilterInputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

class ReferenceFileSource(private val fetcher: UrlFetcher) : FileSource {
    override val isReadOnly: Boolean = false

    override fun close() {
    }

    override fun commit() {
    }

    override fun delete(name: String) {
    }

    override fun lastModified(name: String): Long = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            ZipFile(file).use {
                it.getEntry(entry)?.lastModifiedTime?.to(TimeUnit.MILLISECONDS)
            }
        }
        // url
        testURL(name) -> null
        // file
        testPath(name) && File(name).exists() -> when (val lmod = File(name).lastModified()) {
            0L -> null
            else -> lmod
        }
        // none of above
        else -> error("Not found")
    } ?: -1L

    override fun list(): List<String> = error("Infinite")

    override fun open(name: String, offset: Int): ByteSource = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            source {
                val zf = ZipFile(file)
                val zEnt = zf.getEntry(entry)!!
                val strm = zf.getInputStream(zEnt)!!
                (object : FilterInputStream(strm) {
                    override fun close() {
                        super.close()
                        zf.close()
                    }
                }).skip(offset)
            }
        }
        // url
        testURL(name) -> UrlByteSource(URL(name), offset, fetcher)
        // file
        testPath(name) && File(name).exists() -> FileByteSource(File(name), offset)
        // none of above
        else -> error("Not found")
    }

    override fun put(name: String): ByteSink = error("Read-only")

    override fun size(name: String): Long = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            ZipFile(file).use {
                it.getEntry(entry)?.size
            }
        }
        // url
        testURL(name) -> null
        // file
        testPath(name) && File(name).exists() -> when (val size = File(name).length()) {
            0L -> null
            else -> size
        }
        // none of above
        else -> error("Not found")
    } ?: -1

    override fun has(name: String): Boolean = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            ZipFile(file).use {
                it.getEntry(entry) != null
            }
        }
        // url
        testURL(name) -> fetcher.doHead(URL(name))
        // file
        testPath(name) -> File(name).exists()
        // none of above
        else -> false
    }
}

class InetOnlyFileSource(private val fetcher: UrlFetcher) : FileSource {
    override val isReadOnly: Boolean = false

    override fun close() {
    }

    override fun commit() {
    }

    override fun delete(name: String) {
    }

    override fun lastModified(name: String): Long = size(name)

    override fun open(name: String, offset: Int): ByteSource = when {
        // zip file
        name.startsWith("zip:") -> noIO()
        // url
        testURL(name) -> UrlByteSource(URL(name), offset, fetcher)
        // file
        testPath(name) -> noIO()
        // none of above
        else -> error("Not found")
    }

    override fun size(name: String): Long = when {
        // zip file
        name.startsWith("zip:") -> -1
        // url
        testURL(name) -> -1
        // file
        testPath(name) -> -1
        // none of above
        else -> error("Not found")
    }

    override fun has(name: String): Boolean = when {
        // zip file
        name.startsWith("zip:") -> false
        // url
        testURL(name) -> fetcher.doHead(URL(name))
        // file
        testPath(name) -> false
        // none of above
        else -> false
    }


    override fun list(): List<String> = error("Infinite")

    override fun put(name: String): ByteSink = error("Read-only")

    private fun noIO(): Nothing = error("Filesystem IO is disabled")
}

private fun makeBaseSource(fs: FileSource, allowFsIO: Boolean, fetcher: UrlFetcher): FileSource {
    val b = if (allowFsIO) ReferenceFileSource(fetcher) else InetOnlyFileSource(fetcher)
    val cmb = listOf(fs, b).combined()
    return object : FileSource by cmb {
        override fun open(name: String, offset: Int): ByteSource = when (name) {
            MANIFEST -> fs.open(MANIFEST_INDEX, offset)
            else -> b.open(name, offset)
        }

        override fun open(name: String): ByteSource = when (name) {
            MANIFEST -> fs.open(MANIFEST_INDEX, 0)
            else -> b.open(name, 0)
        }

        override fun has(name: String): Boolean = when (name) {
            MANIFEST -> true
            else -> cmb.has(name)
        }
    }
}

fun FileSource.withV1IndexedEncryption(password: String, allowFsIO: Boolean = false, fetcher: UrlFetcher = UrlFetcher.default): Cryptorage =
        makeBaseSource(this, allowFsIO, fetcher).withV1Encryption(password)

fun FileSource.withV1IndexedEncryption(password: AesKeys, allowFsIO: Boolean = false, fetcher: UrlFetcher = UrlFetcher.default): Cryptorage =
        makeBaseSource(this, allowFsIO, fetcher).withV1Encryption(password)

fun FileSource.withV3IndexedEncryption(password: String, allowFsIO: Boolean = false, fetcher: UrlFetcher = UrlFetcher.default): Cryptorage =
        makeBaseSource(this, allowFsIO, fetcher).withV3Encryption(password, false)

fun FileSource.withV3IndexedEncryption(password: AesKeys, allowFsIO: Boolean = false, fetcher: UrlFetcher = UrlFetcher.default): Cryptorage =
        makeBaseSource(this, allowFsIO, fetcher).withV3Encryption(password, false)
