package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.FilterOutputStream

class FileSourceEditor {
    private val sources: MutableList<FileSource> = mutableListOf()
    private var upper: FileSource? = null
    private val aliases: MutableMap<String, String> = mutableMapOf()

    fun from(fs: FileSource): FileSourceEditor = also {
        sources += fs
    }

    fun upper(fs: FileSource): FileSourceEditor = also {
        upper = fs
    }

    fun alias(src: String, dst: String): FileSourceEditor = also {
        aliases[src] = dst
    }

    fun build(): FileSource {
        val upper = upper
        val base = if (upper == null) {
            sources
        } else {
            listOf(upper) + sources
        }.combined()
        return object : FileSource by base {
            private val deleted: MutableList<String> = mutableListOf()

            override val isReadOnly: Boolean
                get() = upper?.isReadOnly ?: true

            override fun open(name: String, offset: Int): ByteSource = source {
                if (name in deleted) {
                    error("File is already deleted")
                }
                base.open(name, offset).openStream()
            }

            override fun open(name: String): ByteSource = source {
                if (name in deleted) {
                    error("File is already deleted")
                }
                base.open(name).openStream()
            }

            override fun put(name: String): ByteSink = sink {
                object : FilterOutputStream(upper?.put(name)?.openStream() ?: error("Read-only")) {
                    override fun close() {
                        super.close()
                        deleted -= name
                    }
                }
            }

            override fun delete(name: String) {
                upper?.delete(name)
                deleted += name
            }

            override fun commit() {
                base.cryptorages.forEach {
                    try {
                        it.commit()
                    } catch (e: Throwable) {
                    }
                }
            }
        }
    }
}

inline fun editFileSource(f: FileSourceEditor.() -> Unit) = FileSourceEditor().also {
    it.f()
}.build()
