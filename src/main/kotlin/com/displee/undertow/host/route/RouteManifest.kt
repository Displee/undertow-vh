package com.displee.undertow.host.route

import io.undertow.util.Methods

annotation class RouteManifest(val route: String, val method: String = Methods.GET_STRING)