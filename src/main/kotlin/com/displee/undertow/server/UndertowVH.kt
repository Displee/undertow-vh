package com.displee.undertow.server

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.host.VirtualHostManager
import com.displee.undertow.ssl.SSLContextFactory
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

open class UndertowVH(private val host: String, private val port: Int, private val sslPort: Int): HttpHandler {

    private val logger = mu.KotlinLogging.logger {}
    private val virtualHostManager = VirtualHostManager()

    constructor(host: String, port: Int): this(host, port, -1)

    fun start() {
        val hosts = virtualHostManager.hosts.keys
        logger.debug("Initializing UndertowVH with ${hosts.size} hosts: $hosts.")
        val builder = Undertow.builder()
        onBuild(builder)
        val server = builder.build()
        server.start()
        logger.debug("UndertowVH is listening for requests on $host:$port" + (if (sslPort != -1) " and $sslPort" else "") + ".")
    }

    open fun onBuild(builder: Undertow.Builder) {
        val version = System.getProperty("java.version")
        if (!(version[0].toString().toInt() == 1 && version[2].toString().toInt() < 8)) {
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        }
        builder.addHttpListener(port, host)
        if (sslPort != -1) {
            builder.addHttpsListener(sslPort, host, SSLContextFactory.create(virtualHostManager))
        }
        builder.setHandler(this)
    }

    fun register(virtualHost: VirtualHost) {
        if (!virtualHostManager.register(virtualHost)) {
            return
        }
        virtualHost.initialize()
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        val virtualHost = virtualHostManager.resolve(exchange.hostName)
        if (virtualHost == null) {
            logger.warn("No virtual host found for ${exchange.hostName}.")
            return
        }
        virtualHost.handle(exchange)
    }

}