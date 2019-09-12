package com.nao20010128nao.CryptorageExtras

import com.nao20010128nao.Cryptorage.Cryptorage
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import java.io.InputStream
import java.io.OutputStream
import java.lang.management.ManagementFactory

class CryptorageFileSystemView(private val fs: Cryptorage, private val allowClose: Boolean = false) : FileSystemView {
    private val root = RootFtpFile(fs)

    override fun getWorkingDirectory(): FtpFile = root

    override fun isRandomAccessible(): Boolean = true

    override fun getFile(file: String): FtpFile = when (file) {
        "/", "./" -> root
        else -> ActualFtpFile(fs, when (file) {
            "/" -> file.substring(1)
            "./" -> file.substring(2)
            else -> file
        })
    }

    override fun getHomeDirectory(): FtpFile = root

    override fun dispose() {
        if (allowClose) {
            fs.close()
        }
    }

    override fun changeWorkingDirectory(dir: String): Boolean = dir == "/"

    private abstract class AbsFtpFile : FtpFile {

        // reason: Cryptorage does not have links
        override fun getLinkCount(): Int = 0

        // reason: anyone who have can decrypt can access the file, there is no ownership
        override fun getOwnerName(): String = "root"

        // reason: Cryptorage does not hide anything
        override fun isHidden(): Boolean = false

        // reason: what is this?
        override fun getPhysicalFile(): Any = ""

        // reason: Cryptorage does not have directories
        override fun mkdir(): Boolean = false

        // reason: Cryptorage cannot do this
        override fun setLastModified(time: Long): Boolean = false

        // reason: anyone who have can decrypt can access the file, there is no ownership
        override fun getGroupName(): String = "cryptorage"

        // reason: Cryptorage can always read file, if the source is not broken
        override fun isReadable(): Boolean = true
    }

    private class RootFtpFile(private val fs: Cryptorage) : AbsFtpFile() {
        override fun getName(): String = ""

        override fun getSize(): Long = 114514L

        override fun isFile(): Boolean = false

        override fun listFiles(): List<FtpFile> = fs.list().map { ActualFtpFile(fs, it) }

        override fun delete(): Boolean = false

        override fun doesExist(): Boolean = true

        override fun createInputStream(offset: Long): InputStream = error("Opening stream for root")

        override fun getAbsolutePath(): String = "/"

        override fun createOutputStream(offset: Long): OutputStream = error("Opening stream for root")

        override fun isDirectory(): Boolean = true

        override fun isWritable(): Boolean = !fs.isReadOnly

        // reason: Cryptorage does not hold lastModified of its root
        override fun getLastModified(): Long = ManagementFactory.getRuntimeMXBean().startTime

        override fun move(destination: FtpFile?): Boolean = false

        override fun isRemovable(): Boolean = false
    }

    private class ActualFtpFile(private val fs: Cryptorage, fn: String) : AbsFtpFile() {
        private val filename = if (fn.startsWith("/")) fn.substring(1) else fn

        init {
            require(filename != "")
        }

        override fun getName(): String = filename

        override fun getSize(): Long = fs.size(filename)

        override fun isFile(): Boolean = true

        override fun listFiles(): MutableList<out FtpFile>? = null

        override fun delete(): Boolean = try {
            fs.delete(filename)
            true
        } catch (e: Throwable) {
            false
        }

        override fun doesExist(): Boolean = fs.has(filename)

        override fun createInputStream(offset: Long): InputStream = fs.open(filename, offset.toInt()).openStream()

        override fun getAbsolutePath(): String = "/$filename"

        override fun createOutputStream(offset: Long): OutputStream = if (offset == 0L) {
            fs.put(filename).openStream()
        } else {
            error("Appending to the file is not supported, write from the beginning")
        }

        override fun isWritable(): Boolean = !fs.isReadOnly

        override fun isDirectory(): Boolean = false

        override fun getLastModified(): Long = fs.lastModified(filename)

        override fun move(destination: FtpFile): Boolean = if (destination.absolutePath.substring(1).contains("/")) {
            // reason: Cryptorage does not have directories
            false
        } else {
            try {
                fs.mv(filename, destination.absolutePath.substring(1))
                true
            } catch (e: Throwable) {
                false
            }
        }

        override fun isRemovable(): Boolean = !fs.isReadOnly
    }
}