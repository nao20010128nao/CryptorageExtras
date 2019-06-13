@file:Suppress("NOTHING_TO_INLINE", "UnstableApiUsage")

package com.nao20010128nao.CryptorageExtras

import com.google.common.base.Optional
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.google.common.io.CharSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.forCrypto
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream


private fun createCipher(keys: AesKeys, mode: Int): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/Pkcs5Padding")
    val (key, iv) = keys.forCrypto()
    cipher.init(mode, key, iv)
    return cipher
}

internal class AesDecryptorByteSource(private val source: ByteSource, private val keys: AesKeys) : ByteSource() {
    override fun openStream(): InputStream = CipherInputStream(source.openStream(), createCipher(keys, Cipher.DECRYPT_MODE))
}

internal inline fun String.utf8Bytes(): ByteArray = toByteArray(StandardCharsets.UTF_8)

internal inline fun ByteSource.asCharSource(): CharSource = this.asCharSource(StandardCharsets.UTF_8)
internal inline fun ByteArray.digest(algo: String = "sha-256"): ByteArray = MessageDigest.getInstance(algo).digest(this)


internal inline fun ByteArray.leading(n: Int): ByteArray = crop(0, n)

internal inline fun ByteArray.trailing(n: Int): ByteArray = crop(size - n, n)

internal inline fun ByteArray.crop(off: Int, len: Int): ByteArray {
    val result = ByteArray(len)
    System.arraycopy(this, off, result, 0, len)
    return result
}

internal inline fun InputStream.skip(length: Int): InputStream = also {
    ByteStreams.skipFully(it, length.toLong())
}


internal inline fun <T, R> Iterable<T>.firstNonNull(func: (T) -> R?): R? {
    for (i in this) {
        return try {
            func(i)
        } catch (e: Throwable) {
            null
        } ?: continue
    }
    return null
}

internal class FileByteSource(private val file: File, private val offset: Int) : ByteSource() {
    override fun openStream(): InputStream = file.inputStream().skip(offset)

    override fun sizeIfKnown(): Optional<Long> = Optional.of(file.length() - offset)
}

internal class UrlByteSource(private val url: URL, private val offset: Int) : ByteSource() {
    override fun openStream(): InputStream = url.openStream().skip(offset)
}