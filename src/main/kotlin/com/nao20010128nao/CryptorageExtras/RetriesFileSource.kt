package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource

class RetriesFileSource(private val fs: FileSource, private val retry: Int) : FileSource {
    override val isReadOnly: Boolean
        get() = fs.isReadOnly

    override fun close() {
        fs.close()
    }

    override fun commit() {
        fs.commit()
    }

    override fun delete(name: String) {
        fs.delete(name)
    }

    override fun lastModified(name: String): Long = fs.lastModified(name)
    override fun list(): Array<String> = fs.list()
    override fun open(name: String, offset: Int): ByteSource = probable(retry) { fs.open(name, offset) }!!
    override fun size(name: String): Long = fs.size(name)

    override fun put(name: String): ByteSink = if (isReadOnly) {
        error("This FileSource is read-only.")
    } else {
        probable(retry) { fs.put(name) }!!
    }
}