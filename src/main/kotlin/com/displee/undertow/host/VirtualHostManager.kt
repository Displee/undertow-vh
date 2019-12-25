package com.displee.undertow.host

import com.displee.undertow.logger.UndertowVHLogger
import io.undertow.server.RoutingHandler

class VirtualHostManager : RoutingHandler() {

    val hosts = HashMap<String, VirtualHost>()

    public fun register(virtualHost: VirtualHost): Boolean {
        var register = true
        for(hostName in virtualHost.hosts) {
            if (hosts[hostName] == null) {
                continue
            }
            register = false
            UndertowVHLogger.error("There is already a host name registered for host name: $hostName.")
        }
        if (!register) {
            UndertowVHLogger.error("Failed to register virtual host.")
            return false
        }
        for(hostName in virtualHost.hosts) {
            hosts[hostName] = virtualHost
        }
        return true
    }

    public fun resolve(hostName: String) : VirtualHost? {
        val virtualHost = hosts[hostName]
        if (virtualHost == null) {
            UndertowVHLogger.warn("No virtual host found for host name: $hostName.")
            return null
        }
        return virtualHost
    }

}