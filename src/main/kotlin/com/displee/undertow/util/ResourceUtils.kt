package com.displee.undertow.util

import com.displee.io.impl.OutputBuffer
import java.nio.charset.Charset
import java.nio.file.Path

object ResourceUtils {

    private const val DEFAULT_RESOURCE_CAPACITY = 1024 //resources are not that big right?
    private var buffer = ByteArray(DEFAULT_RESOURCE_CAPACITY * 4)
    private var cache = true

    private val cached = HashMap<String, ByteArray>()

    fun cache() {
        cache = true
    }

    fun unCache() {
        cache = false
    }

    fun get(path: Path): ByteArray? {
        check(!path.isAbsolute) { "The path must be relative!" }
        val stringPath = formatPath(path)
        val cachedData = cached[stringPath]
        if (cachedData != null) {
            return cachedData
        }
        val stream = ResourceUtils::class.java.getResourceAsStream(stringPath) ?: return null
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
        val stream = ResourceUtils::class.java.getResourceAsStream(stringPath)
        val exists = stream != null
        if (exists) {
            stream.close()
        }
        return exists
    }

    fun formatPath(path: Path): String {
        return path.toString().replace("\\", "/")
    }

    fun clearCache() {
        cached.clear()
    }

}