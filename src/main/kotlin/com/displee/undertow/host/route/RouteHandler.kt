package com.displee.undertow.host.route

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.logger.UndertowVHLogger
import com.displee.undertow.template.TemplateProcessorFactory
import com.displee.undertow.util.ResourceUtils
import com.google.common.net.MediaType
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.Methods
import org.apache.commons.io.FilenameUtils
import java.nio.file.Files
import java.nio.file.Path

public const val GET = Methods.GET_STRING
public const val POST = Methods.POST_STRING
public const val PUT = Methods.PUT_STRING
public const val DELETE = Methods.DELETE_STRING
public const val PATCH = Methods.PATCH_STRING

abstract class RouteHandler : HttpHandler {

    lateinit var virtualHost: VirtualHost

    protected val model = HashMap<String, Any>()

    override fun handleRequest(exchange: HttpServerExchange) {
        val path = path()
        if (path == null) {
            UndertowVHLogger.error("Path is null!")
            return
        }
        val pathExists = ResourceUtils.exists(path)
        if (!pathExists) {
            UndertowVHLogger.error("Path \"$path\" not found.")
            return
        }
        val extension: String = FilenameUtils.getExtension(path.toString())
        val templateProcessor = TemplateProcessorFactory.get(extension)
        val output: String
        val contentType: String
        if (templateProcessor == null) {
            val data = ResourceUtils.get(path)
            if (data == null) {
                UndertowVHLogger.error("Data for path \"$path\" is null.")
                return
            }
            output = String(data)
            contentType = Files.probeContentType(path) ?: MediaType.HTML_UTF_8.toString()
        } else {
            model(exchange)
            output = templateProcessor.render(path, model)
            contentType = templateProcessor.contentType()
        }
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, contentType)
        exchange.responseSender.send(output)
    }

    public open fun model(exchange: HttpServerExchange) {
        model[REQUEST_URI] = exchange.requestURI
        model[SESSION] = exchange.getSession()
    }

    public open fun path(): Path? {
        return null
    }

    companion object {
        const val REQUEST_URI = "request_uri"
        const val SESSION = "session"
    }

}