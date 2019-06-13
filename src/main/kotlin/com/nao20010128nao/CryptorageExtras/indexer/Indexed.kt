@file:Suppress("unused")

package com.nao20010128nao.CryptorageExtras.indexer

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import com.nao20010128nao.Cryptorage.withV1Encryption
import com.nao20010128nao.CryptorageExtras.*
import com.nao20010128nao.CryptorageExtras.indexer.V1Indexer.Companion.MANIFEST
import com.nao20010128nao.CryptorageExtras.indexer.V1Indexer.Companion.MANIFEST_INDEX
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

object ReferenceFileSource : FileSource {
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
        // file
        testPath(name) && File(name).exists() -> when (val lmod = File(name).lastModified()) {
            0L -> null
            else -> lmod
        }
        // url
        testURL(name) -> null
        // none of above
        else -> error("Not found")
    } ?: -1L

    override fun list(): Array<String> {
        error("Infinity")
    }

    override fun open(name: String, offset: Int): ByteSource = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            object : ByteSource() {
                override fun openStream(): InputStream {
                    val zf = ZipFile(file)
                    val zEnt = zf.getEntry(entry)!!
                    val strm = zf.getInputStream(zEnt)!!
                    return (object : FilterInputStream(strm) {
                        override fun close() {
                            super.close()
                            zf.close()
                        }
                    }).skip(offset)
                }
            }
        }
        // file
        testPath(name) && File(name).exists() -> FileByteSource(File(name), offset)
        // url
        testURL(name) -> UrlByteSource(URL(name), offset)
        // none of above
        else -> error("Not found")
    }

    override fun put(name: String): ByteSink {
        error("Read-only")
    }

    override fun size(name: String): Long = when {
        // zip file
        name.startsWith("zip:") -> {
            val (file, entry) = name.substring(4).split("!")
            ZipFile(file).use {
                it.getEntry(entry)?.size
            }
        }
        // file
        testPath(name) && File(name).exists() -> when (val size = File(name).length()) {
            0L -> null
            else -> size
        }
        // url
        testURL(name) -> null
        // none of above
        else -> error("Not found")
    } ?: -1L
}

object InetOnlyFileSource : FileSource {
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
        // file
        testPath(name) && File(name).exists() -> noIO()
        // url
        testURL(name) -> UrlByteSource(URL(name), offset)
        // none of above
        else -> error("Not found")
    }

    override fun size(name: String): Long = when {
        // zip file
        name.startsWith("zip:") -> -1
        // file
        testPath(name) && File(name).exists() -> -1
        // url
        testURL(name) -> -1
        // none of above
        else -> error("Not found")
    }


    override fun list(): Array<String> {
        error("Infinity")
    }

    override fun put(name: String): ByteSink {
        error("Read-only")
    }

    private fun noIO(): Nothing = error("Filesystem IO is disabled")
}

private fun makeBaseSource(fs: FileSource, allowFsIO: Boolean): FileSource {
    val cmb = listOf(fs, if (allowFsIO) ReferenceFileSource else InetOnlyFileSource).combined()
    return object : FileSource by cmb {
        override fun open(name: String, offset: Int): ByteSource = when (name) {
            MANIFEST_INDEX -> open(MANIFEST, offset)
            else -> cmb.open(name, offset)
        }

        override fun open(name: String): ByteSource = when (name) {
            MANIFEST_INDEX -> open(MANIFEST, 0)
            else -> cmb.open(name, 0)
        }
    }
}

fun FileSource.withV1IndexedEncryption(password: String, allowFsIO: Boolean = false): Cryptorage =
        makeBaseSource(this, allowFsIO).withV1Encryption(password)

fun FileSource.withV1IndexedEncryption(password: AesKeys, allowFsIO: Boolean = false): Cryptorage =
        makeBaseSource(this, allowFsIO).withV1Encryption(password)
