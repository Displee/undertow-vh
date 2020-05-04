package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ResourceUtils
import groovy.text.markup.MarkupTemplateEngine
import java.nio.file.Path

@TemplateProcessorManifest("groovy")
class GroovyTemplateProcessor: TemplateProcessor {

    private val engine = MarkupTemplateEngine()

    override fun render(path: Path, model: Map<String, Any>): String {
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        val template = engine.createTemplate(string)
        return template.make(model).toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}