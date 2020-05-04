package com.displee.undertow.host

import com.displee.undertow.host.route.RouteHandler
import com.displee.undertow.host.route.RouteManifest
import com.displee.undertow.host.route.VirtualHostRouteHandler
import com.displee.undertow.logger.err
import com.displee.undertow.logger.log
import io.undertow.predicate.Predicate
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.resource.PathResourceManager
import io.undertow.server.session.*
import io.undertow.util.HttpString
import io.undertow.util.Methods
import org.reflections.Reflections
import java.nio.file.Path
import java.nio.file.Paths

abstract class VirtualHost(private val name: String, vararg hosts: String) : RoutingHandler() {

    var hosts: Array<String> = arrayOf(*hosts)

    private val sessionManager: SessionManager = InMemorySessionManager(KEY)
    private val sessionConfig: SessionConfig = SessionCookieConfig()
    private val sessionHandler = SessionAttachmentHandler(this, sessionManager, sessionConfig)
    private val resourceManager = PathResourceManager(publicHtml(), 100)

    init {
        if (this.hosts.isEmpty()) {
            this.hosts = arrayOf(name)
        }
        fallbackHandler = VirtualHostRouteHandler(resourceManager)
    }

    fun initialize() {
        routes()
    }

    open fun routes() {
        val reflections = Reflections(javaClass.`package`.name + ".route")
        var count = 0
        val classes = reflections.getSubTypesOf(RouteHandler::class.java)
        for (classz in classes) {
            try {
                val instance = classz.newInstance()
                instance.virtualHost = this
                val manifest = instance.javaClass.getAnnotation(RouteManifest::class.java) ?: continue
                add(HttpString(manifest.method), manifest.route, instance)
                count++
            } catch (t: Throwable) {
                t.printStackTrace()
                err("Unable to instantiate ${classz.simpleName}.")
            }
        }
        log("Registered $count/${classes.size} routes for hosts: ${hosts.contentToString()}.")
    }

    fun handle(exchange: HttpServerExchange) {
        sessionHandler.handleRequest(exchange)
    }

    fun get(template: String?, path: Path): RoutingHandler {
        return get(template, path, mapOf())
    }

    fun get(template: String?, path: Path, model: Map<String, String>): RoutingHandler {
        return get(template, object : RouteHandler() {
            override fun path(): Path? {
                return path
            }

            override fun model(exchange: HttpServerExchange) {
                super.model(exchange)
                this.model.putAll(model)
            }
        })
    }

    @Synchronized
    override fun add(method: HttpString?, template: String?, handler: HttpHandler?): RoutingHandler {
        ensureVirtualHost(handler)
        return super.add(method, template, handler)
    }

    @Synchronized
    override fun add(method: HttpString?, template: String?, predicate: Predicate?, handler: HttpHandler?): RoutingHandler {
        ensureVirtualHost(handler)
        return super.add(method, template, predicate, handler)
    }

    @Synchronized
    fun patch(template: String?, handler: HttpHandler?): RoutingHandler? {
        return add(Methods.PATCH, template, handler)
    }

    private fun ensureVirtualHost(handler: HttpHandler?) {
        if (handler is RouteHandler) {
            handler.virtualHost = this
        }
    }

    fun setDirectoryListingEnabled(directoryListingEnabled: Boolean) {
        (fallbackHandler as VirtualHostRouteHandler).setDirectoryListingEnabled(directoryListingEnabled)
    }

    open fun privateHtml(): Path {
        return documentRoot().resolve("private_html")
    }

    open fun publicHtml(): Path {
        return documentRoot().resolve("public_html")
    }

    open fun sslConfig(): Path {
        return documentRoot().resolve("ssl")
    }

    open fun documentRoot(): Path {
        return Paths.get("/web/$name")
    }

    companion object {
        private const val KEY = "SESSION_MANAGER"
    }

}