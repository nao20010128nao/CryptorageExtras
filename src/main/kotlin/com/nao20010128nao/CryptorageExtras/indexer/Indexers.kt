@file:Suppress("unused")

package com.nao20010128nao.CryptorageExtras.indexer

import com.beust.klaxon.*
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.asFileSource
import com.nao20010128nao.Cryptorage.asZipFileSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import com.nao20010128nao.CryptorageExtras.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URL

interface Indexer {
    fun addIndex(url: URL)
    fun addIndexDirectory(file: File)
    fun addIndexZip(file: File)
    fun list(): List<String>
    fun mv(from: String, to: String)
    fun delete(name: String)
    fun has(name: String): Boolean = list().contains(name)
    fun lastModified(name: String): Long
    fun size(name: String): Long
    fun joinSplits()

    fun serialize(): ByteSource
    fun writeTo(fs: FileSource)
}

class V1Indexer(private val keys: AesKeys) : Indexer {

    constructor(password: String) : this(populateKeys(password))

    private val finalIndex: Index = Index()

    override fun addIndex(url: URL) {
        val fs = url.asFileSource()
        val index = readIndex(fs)
        index.files.forEach { name, file ->
            finalIndex.files[name] = file.copy(files = file.files.map {
                URL(url.protocol, url.host, url.port, "${url.path}/$it${if (url.query.isNullOrBlank()) "" else "?${url.query}"}").toString()
            }.toMutableList())
        }
    }

    override fun addIndexDirectory(file: File) {
        val fs = file.asFileSource()
        val index = readIndex(fs)
        index.files.forEach { name, ff ->
            finalIndex.files[name] = ff.copy(files = ff.files.map { File(file, name).toString() }.toMutableList())
        }
    }

    override fun addIndexZip(file: File) {
        val fs = file.asZipFileSource()
        val index = readIndex(fs)
        index.files.forEach { name, ff ->
            finalIndex.files[name] = ff.copy(files = ff.files.map { "zip:$file!$name" }.toMutableList())
        }
    }

    override fun list(): List<String> = finalIndex.files.keys.toList()

    override fun mv(from: String, to: String) {
        finalIndex.files[to] = finalIndex.files[from]!!
        finalIndex.files.remove(from)
    }

    override fun delete(name: String) {
        finalIndex.files.remove(name)
    }

    override fun joinSplits() {
        val splits = list().filter { splitZeroFilename.matches(it) }.map { it.substring(0, it.length - 10) }.distinct()
        splits.forEach { name ->
            val regex = makeDedicatedSplitFilename(name)
            // find all split files
            val pieces = list().filter { regex.matches(it) }.sorted()
            // enumerate all consisting file
            val allFiles = pieces.flatMap { finalIndex.files[it]!!.files }
            // build a new file
            val newFile = finalIndex.files[pieces[0]]!!.copy(
                    files = allFiles.toMutableList()
            )
            // remove old files
            pieces.forEach(this@V1Indexer::delete)
            // add new file
            finalIndex.files[name] = newFile
        }
    }

    override fun lastModified(name: String): Long = finalIndex.files[name]?.lastModified ?: -1

    override fun size(name: String): Long = finalIndex.files[name]?.size ?: -1

    override fun serialize(): ByteSource = object : ByteSource() {
        override fun openStream(): InputStream {
            val root = JsonObject()
            root["files"] = finalIndex.files.mapValues { it.value.toJsonMap() }
            // empty, but required to load without error
            root["meta"] = emptyMap<String, String>()
            return ByteArrayInputStream(root.toJsonString(false).utf8Bytes())
        }
    }.encrypt(keys)

    override fun writeTo(fs: FileSource) {
        fs.put(MANIFEST_INDEX).writeFrom(serialize().openBufferedStream())
    }

    companion object {
        const val MANIFEST: String = "manifest"
        const val MANIFEST_INDEX: String = "manifest_index"

        private fun populateKeys(password: String): AesKeys {
            val utf8Bytes1 = password.utf8Bytes()
            val utf8Bytes2 = "$password$password".utf8Bytes()
            return utf8Bytes1.digest().digest().leading(16) to utf8Bytes2.digest().digest().trailing(16)
        }
    }

    private fun readIndex(source: FileSource): Index = if (source.has(MANIFEST)) {
        val data = (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST), keys).asCharSource().openStream()) as JsonObject)
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        Index(files.toMutableMap())
    } else {
        Index(hashMapOf())
    }

    private data class Index(val files: MutableMap<String, CryptorageFile> = mutableMapOf())

    private data class CryptorageFile(val files: MutableList<String> = ArrayList(), val splitSize: Int = 0, var lastModified: Long = 0, var size: Long = 0) {
        constructor(file: JsonObject) :
                this(file.array<String>("files")!!.toMutableList(), file.int("splitSize")!!, file.long("lastModified")!!, file.long("size")!!)

        fun toJsonMap(): Map<String, Any> = mapOf(
                "files" to files,
                "splitSize" to splitSize,
                "lastModified" to lastModified,
                "size" to size
        )
    }
}