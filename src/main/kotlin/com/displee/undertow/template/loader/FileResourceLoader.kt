package com.displee.undertow.template.loader

import com.displee.undertow.util.ENCODING
import com.displee.undertow.util.ResourceUtils
import com.mitchellbosecke.pebble.error.LoaderException
import com.mitchellbosecke.pebble.loader.Loader
import com.mitchellbosecke.pebble.utils.PathUtils
import java.io.*
import java.nio.file.Paths

class FileResourceLoader : Loader<String> {

    var prefix: String? = null
        private set
    var suffix: String? = null
        private set

    private var charset = ENCODING

    override fun getReader(templateName: String): Reader {
        val data = ResourceUtils.getAsString(Paths.get(templateName))
            ?: throw LoaderException(null, "Could not find template \"$templateName\".")
        return BufferedReader(StringReader(data))
    }

    override fun setSuffix(suffix: String) {
        this.suffix = suffix
    }

    override fun setPrefix(prefix: String) {
        this.prefix = prefix
    }

    override fun setCharset(charset: String) {
        this.charset = charset
    }

    override fun resolveRelativePath(relativePath: String, anchorPath: String): String {
        val resourcePath = PathUtils.resolveRelativePath(relativePath, anchorPath, File.separatorChar)
        check(resourcePath != null && ResourceUtils.exists(Paths.get(resourcePath))) { "Resource \"${resourcePath ?: relativePath}\" doesn't exist." }
        return resourcePath
    }

    override fun createCacheKey(templateName: String): String {
        return templateName
    }

    override fun resourceExists(templateName: String): Boolean {
        return ResourceUtils.exists(Paths.get(templateName))
    }

}