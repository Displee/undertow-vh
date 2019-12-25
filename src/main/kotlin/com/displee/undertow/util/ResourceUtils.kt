package com.displee.undertow.util

import com.displee.io.impl.OutputBuffer
import java.nio.file.Path

class ResourceUtils {

    companion object {

        private const val DEFAULT_RESOURCE_CAPACITY = 1024//resources are not that big right?
        private var buffer = ByteArray(DEFAULT_RESOURCE_CAPACITY * 4)
        private var cache = true

        private val cached = HashMap<String, ByteArray>()

        public fun cache() {
            cache = true
        }

        public fun unCache() {
            cache = false
        }

        public fun get(path: Path): ByteArray? {
            val stringPath = formatPath(path)
            val cachedData = cached[stringPath]
            if (cachedData != null) {
                return cachedData
            }
            val stream = ResourceUtils::class.java.getResourceAsStream(stringPath) ?: return null
            val output = OutputBuffer(DEFAULT_RESOURCE_CAPACITY)
            var length: Int
            while((stream.read(buffer).also { length = it }) > 0) {
                output.write(buffer, 0, length)
            }
            stream.close()
            val data = output.array()
            if (cache) {
                cached[stringPath] = data
            }
            return data
        }

        public fun exists(path: Path): Boolean {
            val stringPath = formatPath(path)
            val stream = ResourceUtils::class.java.getResourceAsStream(stringPath)
            val exists = stream != null
            if (exists) {
                stream.close()
            }
            return exists
        }

        public fun formatPath(path: Path): String {
            return path.toString().replace("\\", "/")
        }

        public fun clearCache() {
            cached.clear()
        }

    }

}