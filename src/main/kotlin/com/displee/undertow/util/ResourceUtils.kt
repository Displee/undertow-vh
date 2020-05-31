package com.displee.undertow.util

import com.displee.io.impl.OutputBuffer
import java.io.File
import java.io.InputStream
import java.nio.file.Path

object ResourceUtils {

    private const val RESOURCE_SEPARATOR = '/'
    private const val DEFAULT_RESOURCE_CAPACITY = 1024 //resources are not that big right?
    private var buffer = ByteArray(DEFAULT_RESOURCE_CAPACITY * 4)
    var cache = true

    private val cached = HashMap<String, ByteArray>()

    private fun loadResource(path: String): InputStream? {
        val resource = this.javaClass.classLoader.getResource(path.substring(1)) ?: return null
        val connection = resource.openConnection()
        connection.useCaches = cache
        return connection.getInputStream()
    }

    fun get(path: Path): ByteArray? {
        check(!path.isAbsolute) { "The path must be relative!" }
        val stringPath = formatPath(path)
        val cachedData = cached[stringPath]
        if (cachedData != null) {
            return cachedData
        }
        val stream = loadResource(stringPath) ?: return null
        val output = OutputBuffer(DEFAULT_RESOURCE_CAPACITY)
        var length: Int
        while((stream.read(buffer).also { length = it }) > 0) {
            output.writeBytes(buffer, 0, length)
        }
        stream.close()
        val data = output.array()
        if (cache) {
            cached[stringPath] = data
        }
        return data
    }

    fun getAsString(path: Path): String? {
        val data = get(path) ?: return null
        return String(data, CHARSET)
    }

    fun exists(path: Path): Boolean {
        val stringPath = formatPath(path)
        val stream = loadResource(stringPath)
        stream?.close()
        return stream != null
    }

    fun formatPath(path: Path): String {
        var p = path.toString().replace(File.separatorChar, RESOURCE_SEPARATOR)
        if (!p.startsWith(RESOURCE_SEPARATOR)) {
            p = "$RESOURCE_SEPARATOR$p"
        }
        return p
    }

    fun clearCache() {
        cached.clear()
    }

}