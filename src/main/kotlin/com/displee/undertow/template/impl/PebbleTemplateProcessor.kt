package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ResourceUtils
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.StringLoader
import java.io.StringWriter
import java.nio.file.Path

@TemplateProcessorManifest("peb", "twig")
class PebbleTemplateProcessor: TemplateProcessor {

    private val engine = PebbleEngine.Builder().loader(StringLoader()).build()

    override fun render(path: Path, model: Map<String, Any>): String {
        check(!path.isAbsolute) { "The path must be relative!" }
        val data = ResourceUtils.get(path)
        checkNotNull(data) { "No file data." }
        val template = engine.getTemplate(String(data))
        val writer = StringWriter()
        template.evaluate(writer, model)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}