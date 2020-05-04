package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.LOCALE
import com.displee.undertow.util.ResourceUtils
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.StringWriter
import java.nio.file.Path

//we need to distinguish between extensions, for thymeleaf we use ".tl" extension
@TemplateProcessorManifest("tl")
class ThymeleafTemplateProcessor: TemplateProcessor {

    private val engine = TemplateEngine()

    override fun render(path: Path, model: Map<String, Any>): String {
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        val writer = StringWriter()
        engine.process(string, Context(LOCALE, model), writer)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

}