package com.displee.undertow.server

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.host.VirtualHostManager
import com.displee.undertow.logger.UndertowVHLogger
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class UndertowVH(private val host: String, private val port: Int, private val sslPort: Int): HttpHandler {

    private val virtualHostManager = VirtualHostManager()

    constructor(host: String, port: Int): this(host, port, -1)

    public fun initialize() {
        val hosts = virtualHostManager.hosts.keys
        UndertowVHLogger.log("Initializing UndertowVH with ${hosts.size} hosts: $hosts.")
        val builder = Undertow.builder()
        onBuild(builder)
        val server = builder.build()
        server.start()
        UndertowVHLogger.log("UndertowVH is listening for requests on $host:$port" + (if (sslPort != -1) " and $sslPort" else "") + ".")
    }

    public open fun onBuild(builder: Undertow.Builder) {
        val version = System.getProperty("java.version")
        if (!(version[0] == '1' && Integer.parseInt(version[2] + "") < 8)) {
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        }
        builder.addHttpListener(port, host)
        if (sslPort != -1) {
            //TODO SSL support
            //val sslContext = SSLContextFactory.create(virtualHostManager)
            //builder.addHttpsListener(port, host, sslContext)
        }
        builder.setHandler(this)
    }

    public fun register(virtualHost: VirtualHost) {
        if (!virtualHostManager.register(virtualHost)) {
            return
        }
        virtualHost.initialize()
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        val virtualHost = virtualHostManager.resolve(exchange.hostName)
        if (virtualHost == null) {
            UndertowVHLogger.log("No virtual host found for ${exchange.hostName}.")
            return
        }
        virtualHost.handle(exchange)
    }

    companion object {
        public fun disableLogging() {
            UndertowVHLogger.disableXnioLogger()
        }
    }

}