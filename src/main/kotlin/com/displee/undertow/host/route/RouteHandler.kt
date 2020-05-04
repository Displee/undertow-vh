package com.displee.undertow.host.route

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.logger.err
import com.displee.undertow.template.TemplateProcessorAdapter
import com.displee.undertow.util.CHARSET
import com.displee.undertow.util.ENCODING
import com.displee.undertow.util.ResourceUtils
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.Methods
import org.apache.commons.io.FilenameUtils
import java.nio.file.Files
import java.nio.file.Path

const val GET = Methods.GET_STRING
const val POST = Methods.POST_STRING
const val PUT = Methods.PUT_STRING
const val DELETE = Methods.DELETE_STRING
const val PATCH = Methods.PATCH_STRING

abstract class RouteHandler : HttpHandler {

    lateinit var virtualHost: VirtualHost

    protected val model = HashMap<String, Any>()

    override fun handleRequest(exchange: HttpServerExchange) {
        val path = path()
        if (path == null) {
            err("Path is null.")
            return
        }
        val pathExists = ResourceUtils.exists(path)
        if (!pathExists) {
            err("Path \"$path\" not found.")
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
        model[REQUEST_URI] = exchange.requestURI
        model[SESSION] = exchange.getSession()
    }

    open fun path(): Path? {
        return null
    }

    companion object {
        const val REQUEST_URI = "request_uri"
        const val SESSION = "session"
    }

}