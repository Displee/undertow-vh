package com.displee.web.localhost.route

import com.displee.undertow.host.route.*
import com.displee.undertow.host.route.impl.TemplateRouteHandler
import com.google.gson.JsonObject
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods

@RouteManifest("/register", Methods.GET_STRING)
class SampleRoute : TemplateRouteHandler() {

    override fun handleRequest(exchange: HttpServerExchange) {
        //check if we've received the required query params
        if (!exchange.checkQueryParameters("username", "first_name", "last_name")) {
            exchange.send("Error received not enough parameters.")
            return
        }
        val queryParameters = exchange.getQueryParametersAsMap()
        val username = queryParameters["username"]
        val firstName = queryParameters["first_name"]
        val lastName = queryParameters["last_name"]
        //we can do any logic here, for example put the data above in a database
        val json = JsonObject()
        json.addProperty("error", -1)
        json.addProperty("message", "Successfully registered!")
        exchange.send(json)
    }

}