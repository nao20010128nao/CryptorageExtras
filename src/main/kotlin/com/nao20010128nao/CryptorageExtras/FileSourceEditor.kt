package com.nao20010128nao.CryptorageExtras

import com.nao20010128nao.Cryptorage.internal.file.*

class FileSourceEditor{
    private val sources:MutableList<FileSource> = mutableListOf()
    private var upper:FileSource? = null
    private val aliases:MutableMap<String,String> = mutableMapOf()

    fun from(fs:FileSource):FileSourceEditor=also{
        sources+=fs
    }
    fun upper(fs:FileSource):FileSourceEditor=also{
        upper=fs
    }
    fun alias(src:String,dst:String):FileSourceEditor=also{
        aliases[src]=dst
    }
    fun build():FileSource=error("unimplemented")
}

inline fun editFileSource(f:FileSourceEditor.()->Unit)=FileSourceEditor().also{
    it.f()
}.build()
