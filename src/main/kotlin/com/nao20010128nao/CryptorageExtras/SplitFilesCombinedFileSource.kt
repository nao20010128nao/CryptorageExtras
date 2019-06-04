package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.InputStream

class SplitFilesCombinedFileSource(private val fs: FileSource) : ExtendedFileSource {

    override val isReadOnly: Boolean = fs.isReadOnly

    override fun close() {
        Cryptorage
        fs.close()
    }

    override fun commit() {
        fs.commit()
    }

    override fun delete(name: String) {
        if (fs.has("$name.000.split")) {
            val regex = makeDedicatedSplitFilename(name)
            fs.list().filter { regex.matches(it) }.forEach {
                fs.delete(it)
            }
        } else {
            fs.delete(name)
        }
    }

    // note: this filters very roughly
    override fun list(): Array<String> {
        val lst = fs.list()
        // find all split file and remove its tail
        val wasSplit = lst.asSequence().filter { splitZeroFilename.matches(it) }.map { it.substring(0, it.length - 10) }
        // find all non-split file
        val wasntSplit = lst.asSequence().filter { !splitFilename.matches(it) }
        // combine without duplication
        return (wasSplit + wasntSplit).distinct().toList().toTypedArray()
    }

    override fun open(name: String, offset: Int): ByteSource = if (fs.has("$name.000.split")) {
        CombinedByteSource(fs, name, offset)
    } else {
        fs.open(name, offset)
    }

    override fun put(name: String): ByteSink = fs.put(name)

    override fun has(name: String): Boolean = fs.has(name)

    private class CombinedByteSource(private val fs: FileSource, name: String, private val offset: Int) :
        ByteSource() {
        val regex = makeDedicatedSplitFilename(name)
        override fun openStream(): InputStream = fs
            .list().asSequence().filter { regex.matches(it) }
            .sorted().map { fs.open(it).openStream() }
            .iterator().combined().skip(offset)
    }

    override fun size(name: String): Long? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}