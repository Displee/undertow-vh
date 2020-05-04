package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ResourceUtils
import de.neuland.jade4j.Jade4J
import java.io.StringReader
import java.nio.file.Path

@TemplateProcessorManifest("jade")
class JadeTemplateProcessor: TemplateProcessor {

    override fun render(path: Path, model: Map<String, Any>): String {
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        return Jade4J.render(StringReader(string), path.toString(), model)
    }

    override fun contentType(): String {
        return "text/html"
    }

}