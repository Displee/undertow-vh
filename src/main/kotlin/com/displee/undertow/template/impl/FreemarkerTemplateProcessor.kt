package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ENCODING
import com.displee.undertow.util.ResourceUtils
import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import java.io.StringWriter
import java.nio.file.Path
import java.util.*

@TemplateProcessorManifest("ftl")
class FreemarkerTemplateProcessor : TemplateProcessor {

    private val engine = buildEngine()

    override fun render(path: Path, model: Map<String, Any>): String {
        val templateLoader = engine.templateLoader as StringTemplateLoader
        if (templateLoader.findTemplateSource(path.toString()) == null) {
            val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
            templateLoader.putTemplate(path.toString(), string)
        }
        val template = engine.getTemplate(path.toString())
        val writer = StringWriter()
        template.process(model, writer)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

    companion object {
        fun buildEngine(): Configuration {
            val configuration = Configuration(Configuration.VERSION_2_3_30)
            configuration.templateLoader = StringTemplateLoader()
            configuration.defaultEncoding = ENCODING
            configuration.locale = Locale.US
            return configuration
        }
    }

}