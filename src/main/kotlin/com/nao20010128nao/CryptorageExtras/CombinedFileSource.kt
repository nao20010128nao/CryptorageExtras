package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.FileSource

class CombinedFileSource(val cryptorages: List<FileSource>) : FileSource {
    constructor(vararg cs: FileSource) : this(cs.asList())

    override val isReadOnly: Boolean = true
    override fun list(): List<String> = cryptorages.asSequence()
            .flatMap { it.list().asSequence() }
            .distinct()
            .toList()

    override fun open(name: String, offset: Int): ByteSource = source {
        cryptorages.firstNonNull { wish { it.open(name, offset).openStream() } }!!
    }

    override fun open(name: String): ByteSource = source {
        cryptorages.firstNonNull { wish { it.open(name).openStream() } }!!
    }

    override fun has(name: String): Boolean = cryptorages.fold(false) { acc, obj -> acc or obj.has(name) }
    override fun lastModified(name: String): Long = cryptorages.firstNonNull {
        it.size(name).let { aa -> if (aa < 0) null else aa }
    } ?: -1

    override fun size(name: String): Long = cryptorages.firstNonNull {
        it.size(name).let { aa -> if (aa < 0) null else aa }
    } ?: -1

    override fun close() = cryptorages.forEach { it.close() }


    override fun put(name: String): ByteSink = error("Read-only")
    override fun delete(name: String) = error("Read-only")
    override fun commit() = error("Read-only")
}
