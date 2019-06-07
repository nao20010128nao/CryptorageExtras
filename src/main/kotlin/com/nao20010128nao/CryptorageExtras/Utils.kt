@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.nao20010128nao.CryptorageExtras

import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.internal.ConcatenatedInputStream
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.InputStream

fun FileSource.withNamePrefixed(name: String): FileSource = PrefixedByFileSource(this, name)

fun FileSource.withSplitFilesCombined(): FileSource = SplitFilesCombinedFileSource(this)


internal val splitFilename: Regex = Regex("(.+)\\.[0-9]{3}\\.split$")
internal val splitZeroFilename: Regex = Regex("(.+)\\.000\\.split$")

internal inline fun makeDedicatedSplitFilename(name: String): Regex = Regex(Regex.escape(name) + "\\.[0-9]{3}\\.split$")

internal inline fun Iterator<InputStream>.combined(): InputStream = ConcatenatedInputStream(this)

internal inline fun InputStream.skip(length: Int): InputStream = also {
    ByteStreams.skipFully(this, length.toLong())
}
