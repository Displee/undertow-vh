package com.displee.undertow.template.impl

import com.displee.undertow.template.TemplateProcessor
import com.displee.undertow.template.TemplateProcessorManifest
import com.displee.undertow.util.ResourceUtils
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
import java.nio.file.Path

@TemplateProcessorManifest("vm")
class VelocityTemplateProcessor: TemplateProcessor {

    override fun render(path: Path, model: Map<String, Any>): String {
        val string = ResourceUtils.getAsString(path) ?: throw IllegalStateException("String is null.")
        val writer = StringWriter()
        Velocity.evaluate(VelocityContext(model), writer, path.toString(), string)
        return writer.toString()
    }

    override fun contentType(): String {
        return "text/html"
    }

    companion object {
        init {
            Velocity.init()
        }
    }

}