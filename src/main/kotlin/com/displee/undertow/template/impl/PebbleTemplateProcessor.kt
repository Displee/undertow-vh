package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.template.loader.FileResourceLoader
import com.mitchellbosecke.pebble.PebbleEngine
import java.io.StringWriter
import java.nio.file.Path

@TemplateProcessorManifest("peb", "twig")
class PebbleTemplateProcessor: TemplateProcessor {

    private val engine = PebbleEngine.Builder().loader(FileResourceLoader()).build()

    override fun render(path: Path, model: Map<String, Any>): String {
        val template = engine.getTemplate(path.toString())
        val writer = StringWriter()
        template.evaluate(writer, model)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}