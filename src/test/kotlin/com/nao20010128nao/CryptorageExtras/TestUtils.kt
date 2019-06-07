package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import com.nao20010128nao.Cryptorage.newMemoryFileSource
import com.nao20010128nao.Cryptorage.withV1Encryption

val zeroBytes = byteArrayOf()

fun FileSource.fakeWrap(): Cryptorage {
    return object : Cryptorage {
        override val isReadOnly: Boolean
            get() = this@fakeWrap.isReadOnly

        override fun close() {
            this@fakeWrap.close()
        }

        override fun commit() {
            this@fakeWrap.commit()
        }

        override fun delete(name: String) {
            this@fakeWrap.delete(name)
        }

        override fun gc() {
        }

        override fun lastModified(name: String): Long = this@fakeWrap.lastModified(name)

        override fun list(): Array<String> = this@fakeWrap.list()

        override fun meta(key: String): String? = null

        override fun meta(key: String, value: String) {
        }

        override fun mv(from: String, to: String) {
        }

        override fun open(name: String, offset: Int): ByteSource = this@fakeWrap.open(name, offset)

        override fun put(name: String): ByteSink = this@fakeWrap.put(name)

        override fun size(name: String): Long = this@fakeWrap.size(name)
    }
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

fun Map<String, ByteArray>.createV1(): Cryptorage {
    val ms = emptyMap<String, ByteArray>().newMemoryFileSource()
    val v1 = ms.withV1Encryption("test")
    v1.meta(Cryptorage.META_SPLIT_SIZE, "200")
    for ((k, v) in this) {
        v1.put(k).write(v)
    }
    v1.commit()
    return v1
}