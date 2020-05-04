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
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        val template = engine.getTemplate(string)
        val writer = StringWriter()
        template.evaluate(writer, model)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}