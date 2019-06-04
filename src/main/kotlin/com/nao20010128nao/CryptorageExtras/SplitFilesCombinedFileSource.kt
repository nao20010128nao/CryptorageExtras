package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.InputStream
import java.lang.Float.NaN

class SplitFilesCombinedFileSource(private val fs: FileSource) : FileSource {

    override val isReadOnly: Boolean = fs.isReadOnly

    override fun close() {
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

    override fun size(name: String): Long = if (fs.has("$name.000.split")) {
        val regex = makeDedicatedSplitFilename(name)
        fs.list().asSequence().filter { regex.matches(it) }
                .map { fs.size(it).toFloat() }.map { if (it < 0) NaN else it }
                .sum().let { if (it.isNaN()) -1 else it.toLong() }
    } else {
        fs.size(name)
    }

    override fun lastModified(name: String): Long = if (fs.has("$name.000.split")) {
        fs.lastModified("$name.000.split")
    } else {
        fs.lastModified(name)
    }
}