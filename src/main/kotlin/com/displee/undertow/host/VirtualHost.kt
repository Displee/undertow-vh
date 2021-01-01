package com.displee.undertow.host

import com.displee.undertow.host.route.*
import com.displee.undertow.host.route.impl.TemplateRouteHandler
import io.undertow.predicate.Predicate
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.server.session.*
import io.undertow.util.HttpString
import io.undertow.util.Methods
import org.reflections.Reflections
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths

abstract class VirtualHost(private val name: String, vararg hosts: String) : RoutingHandler() {

    private val logger = mu.KotlinLogging.logger {}

    var hosts: Array<String> = arrayOf(*hosts)

    private val sessionManager: SessionManager = InMemorySessionManager(KEY)
    private val sessionConfig: SessionConfig = SessionCookieConfig()
    private val sessionHandler = SessionAttachmentHandler(this, sessionManager, sessionConfig)
    private val resourceManager = ClassPathResourceManager(ClassLoader.getSystemClassLoader(), publicHtml().toString().replace("\\", "/"))
    private val resourceHandler = VirtualHostRouteHandler(resourceManager)
    var pageNotFoundHandler = HttpHandler { it.send("404 page not found.") }

    init {
        if (this.hosts.isEmpty()) {
            this.hosts = arrayOf(name)
        }
        fallbackHandler = HttpHandler {
            if (it.isInIoThread) {
                it.dispatch(this)
                return@HttpHandler
            }
            resourceHandler.handleRequest(it)
            if (it.isComplete) {
                return@HttpHandler
            }
            pageNotFoundHandler.handleRequest(it)
            return@HttpHandler
        }
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        val form = exchange.parseFormDataAsMap()
        val _method = form["_method"]
        if (_method != null && _method.isNotEmpty()) {
            val method = Methods.fromString(_method)
            if (method != null) {
                exchange.requestMethod = method
            }
        }
        super.handleRequest(exchange)
    }

    fun initialize() {
        routes()
    }

    open fun routes() {
        val reflections = Reflections(javaClass.`package`.name + ".route")
        var count = 0
        val classes = reflections.getSubTypesOf(HttpHandler::class.java)
        for (classz in classes) {
            if (classz.isInterface || Modifier.isAbstract(classz.modifiers)) {
                continue
            }
            val manifest = classz.getAnnotation(RouteManifest::class.java) ?: continue
            try {
                val instance = classz.newInstance()
                if (instance is VirtualHostRoute) {
                    instance.virtualHost = this
                }
                add(HttpString(manifest.method), manifest.route, instance)
                count++
            } catch (t: Throwable) {
                t.printStackTrace()
                logger.error("Unable to instantiate ${classz.simpleName}.")
            }
        }
        logger.debug("Registered $count/${classes.size} routes for hosts: ${hosts.contentToString()}.")
    }

    fun handle(exchange: HttpServerExchange) {
        sessionHandler.handleRequest(exchange)
    }

    fun get(template: String?, path: Path): RoutingHandler {
        return get(template, path, mapOf())
    }

    fun get(template: String?, path: Path, subHandler: (it: HttpServerExchange, parent: MutableMap<String, Any>) -> Unit): RoutingHandler {
        return get(template, object : TemplateRouteHandler() {
            override fun handleRequest(exchange: HttpServerExchange) {
                subHandler(exchange, this.model)
                super.handleRequest(exchange)
            }

            override fun path(): Path? {
                return path
            }

            override fun model(exchange: HttpServerExchange) {
                super.model(exchange)
                this.model.putAll(model)
            }
        })
    }

    fun get(template: String?, path: Path, model: Map<String, String>): RoutingHandler {
        return get(template, object : TemplateRouteHandler() {
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
        if (handler is TemplateRouteHandler) {
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
        return Paths.get("web", name)
    }

    companion object {
        private const val KEY = "SESSION_MANAGER"
    }

}