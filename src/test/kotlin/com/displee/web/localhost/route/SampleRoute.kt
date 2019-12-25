package com.displee.web.localhost.route

import com.displee.undertow.host.route.GET
import com.displee.undertow.host.route.RouteHandler
import com.displee.undertow.host.route.RouteManifest
import io.undertow.server.HttpServerExchange
import java.nio.file.Path

@RouteManifest("/", GET)
class SampleRoute : RouteHandler() {

    override fun model(exchange: HttpServerExchange) {
        super.model(exchange)
        model["jwz"] = "pebble works!"
    }

    override fun path(): Path? {
        return virtualHost.privateHtml().resolve("test.peb")
    }

}