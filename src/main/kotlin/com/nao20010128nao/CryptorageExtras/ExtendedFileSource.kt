package com.nao20010128nao.CryptorageExtras

import com.nao20010128nao.Cryptorage.internal.file.FileSource

interface ExtendedFileSource : FileSource {
    fun size(name: String): Long?
}