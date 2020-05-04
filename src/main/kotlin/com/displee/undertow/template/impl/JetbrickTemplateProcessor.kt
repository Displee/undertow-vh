package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ResourceUtils
import jetbrick.template.JetEngine
import java.io.StringWriter
import java.nio.file.Path

@TemplateProcessorManifest("jetx")
class JetbrickTemplateProcessor: TemplateProcessor {

    private val engine = JetEngine.create()

    override fun render(path: Path, model: Map<String, Any>): String {
        //TODO Use string template loader
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        val template = engine.getTemplate(path.toString())
        val writer = StringWriter()
        template.render(model, writer)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}