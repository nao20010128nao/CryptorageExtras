package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource

class PrefixedByFileSource(private val fs: FileSource, private val prefix: String) : FileSource {

    override val isReadOnly: Boolean
        get() = fs.isReadOnly

    override fun close() {
        fs.close()
    }

    override fun commit() {
        fs.commit()
    }

    override fun delete(name: String) {
        fs.delete("$prefix$name")
    }

    override fun list(): Array<String> =
            fs.list().filter { it.startsWith(prefix) }.map { it.substring(prefix.length) }.toTypedArray()

    override fun open(name: String, offset: Int): ByteSource = fs.open("$prefix$name", offset)
    override fun put(name: String): ByteSink = fs.put("$prefix$name")
    override fun has(name: String): Boolean = fs.has("$prefix$name")
    override fun size(name: String): Long = fs.size("$prefix$name")
    override fun lastModified(name: String): Long = fs.lastModified("$prefix$name")
}