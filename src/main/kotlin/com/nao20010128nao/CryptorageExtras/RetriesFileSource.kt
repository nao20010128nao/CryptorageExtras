package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.FileSource

class RetriesFileSource(private val fs: FileSource, private val retry: Int) : FileSource by fs {
    override fun open(name: String, offset: Int): ByteSource = probable(retry) { fs.open(name, offset) }!!

    override fun put(name: String): ByteSink = if (isReadOnly) {
        error("This FileSource is read-only.")
    } else {
        probable(retry) { fs.put(name) }!!
    }
}