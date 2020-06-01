package com.displee.undertow.host.route.impl

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.host.route.getSession
import com.displee.undertow.template.TemplateProcessorAdapter
import com.displee.undertow.util.CHARSET
import com.displee.undertow.util.ENCODING
import com.displee.undertow.util.ResourceUtils
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import org.apache.commons.io.FilenameUtils
import java.nio.file.Files
import java.nio.file.Path

abstract class TemplateRouteHandler : HttpHandler {

    private val logger = mu.KotlinLogging.logger {}

    lateinit var virtualHost: VirtualHost

    protected val model = HashMap<String, Any>()

    override fun handleRequest(exchange: HttpServerExchange) {
        val path = path()
        if (path == null) {
            logger.error("Path is null.")
            return
        }
        if (!ResourceUtils.exists(path)) {
            logger.error("Path \"$path\" not found.")
            return
        }
        val extension: String = FilenameUtils.getExtension(path.toString())
        val templateProcessor = TemplateProcessorAdapter.get(extension)
        val output: String
        val contentType: String
        if (templateProcessor == null) {
            val string = ResourceUtils.getAsString(path)
            checkNotNull(string) { "String is null." }
            output = string
            contentType = Files.probeContentType(path) ?: ENCODING
        } else {
            model(exchange)
            output = templateProcessor.render(path, model)
            contentType = templateProcessor.contentType()
        }
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, contentType)
        exchange.responseSender.send(output, CHARSET)
    }

    open fun model(exchange: HttpServerExchange) {
        model[EXCHANGE] = exchange
        model[SESSION] = exchange.getSession()
    }

    open fun path(): Path? {
        return null
    }

    companion object {
        private const val EXCHANGE = "exchange"
        private const val SESSION = "session"
    }

}