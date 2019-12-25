/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.displee.undertow.host.route

import com.displee.undertow.template.TemplateProcessorFactory
import io.undertow.UndertowLogger
import io.undertow.io.IoCallback
import io.undertow.predicate.Predicate
import io.undertow.predicate.Predicates
import io.undertow.server.HandlerWrapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.server.handlers.builder.HandlerBuilder
import io.undertow.server.handlers.cache.ResponseCache
import io.undertow.server.handlers.encoding.ContentEncodedResourceManager
import io.undertow.server.handlers.resource.*
import io.undertow.util.*
import io.undertow.util.ByteRange.RangeResponseResult
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * This is a copy of undertow's {@code io.undertow.server.handlers.resource.ResourceHandler} with a small code addition (lines 266-276).
 * @author Stuart Douglas
 * @author Displee
 */
class VirtualHostRouteHandler : HttpHandler {

    companion object {
        /**
         * Set of methods prescribed by HTTP 1.1. If request method is not one of those, handler will
         * return NOT_IMPLEMENTED.
         */
        private val KNOWN_METHODS: MutableSet<HttpString> = HashSet()

        init {
            KNOWN_METHODS.add(Methods.OPTIONS)
            KNOWN_METHODS.add(Methods.GET)
            KNOWN_METHODS.add(Methods.HEAD)
            KNOWN_METHODS.add(Methods.POST)
            KNOWN_METHODS.add(Methods.PUT)
            KNOWN_METHODS.add(Methods.DELETE)
            KNOWN_METHODS.add(Methods.TRACE)
            KNOWN_METHODS.add(Methods.CONNECT)
        }
    }

    private val welcomeFiles: MutableList<String> =
        CopyOnWriteArrayList(arrayOf("index.html", "index.htm", "default.html", "default.htm"))
    /**
     * If directory listing is enabled.
     */
    @Volatile
    var isDirectoryListingEnabled = false
        private set
    /**
     * If this handler should use canonicalized paths.
     *
     * WARNING: If this is not true and [io.undertow.server.handlers.CanonicalPathHandler] is not installed in
     * the handler chain then is may be possible to perform a directory traversal attack. If you set this to false make
     * sure you have some kind of check in place to control the path.
     * @param canonicalizePaths If paths should be canonicalized
     */
    /**
     * If the canonical version of paths should be passed into the resource manager.
     */
    @Volatile
    var isCanonicalizePaths = true
    /**
     * The mime mappings that are used to determine the content type.
     */
    @Volatile
    var mimeMappings = MimeMappings.DEFAULT
        private set
    @Volatile
    var cachable = Predicates.truePredicate()
        private set
    @Volatile
    var allowed = Predicates.truePredicate()
        private set
    @Volatile
    var resourceSupplier: ResourceSupplier? = null
        private set
    @Volatile
    var resourceManager: ResourceManager? = null
        private set
    /**
     * If this is set this will be the maximum time (in seconds) the client will cache the resource.
     *
     *
     * Note: Do not set this for private resources, as it will cause a Cache-Control: public
     * to be sent.
     *
     *
     * TODO: make this more flexible
     *
     *
     * This will only be used if the [.cachable] predicate returns true
     */
    @Volatile
    var cacheTime: Int? = null
        private set
    @Volatile
    var contentEncodedResourceManager: ContentEncodedResourceManager? = null
        private set
    /**
     * Handler that is called if no resource is found
     */
    private val next: HttpHandler

    constructor(resourceSupplier: ResourceManager?) : this(
        resourceSupplier,
        ResponseCodeHandler.HANDLE_404
    ) {
    }

    constructor(
        resourceManager: ResourceManager?,
        next: HttpHandler
    ) {
        resourceSupplier = DefaultResourceSupplier(resourceManager)
        this.resourceManager = resourceManager
        this.next = next
    }

    constructor(resourceSupplier: ResourceSupplier?) : this(resourceSupplier, ResponseCodeHandler.HANDLE_404) {}
    constructor(resourceManager: ResourceSupplier?, next: HttpHandler) {
        resourceSupplier = resourceManager
        this.next = next
    }

    /**
     * You should use [] instead.
     */
    @Deprecated("")
    constructor() {
        next = ResponseCodeHandler.HANDLE_404
    }

    @Throws(Exception::class)
    override fun handleRequest(exchange: HttpServerExchange) {
        if (exchange.requestMethod.equals(Methods.GET) ||
            exchange.requestMethod.equals(Methods.POST)
        ) {
            serveResource(exchange, true)
        } else if (exchange.requestMethod.equals(Methods.HEAD)) {
            serveResource(exchange, false)
        } else {
            if (KNOWN_METHODS.contains(exchange.requestMethod)) {
                exchange.statusCode = StatusCodes.METHOD_NOT_ALLOWED
                exchange.responseHeaders.add(
                    Headers.ALLOW,
                    java.lang.String.join(", ", Methods.GET_STRING, Methods.HEAD_STRING, Methods.POST_STRING)
                )
            } else {
                exchange.statusCode = StatusCodes.NOT_IMPLEMENTED
            }
            exchange.endExchange()
        }
    }

    @Throws(Exception::class)
    private fun serveResource(exchange: HttpServerExchange, sendContent: Boolean) {
        if (DirectoryUtils.sendRequestedBlobs(exchange)) {
            return
        }
        if (!allowed.resolve(exchange)) {
            exchange.statusCode = StatusCodes.FORBIDDEN
            exchange.endExchange()
            return
        }
        val cache =
            exchange.getAttachment(ResponseCache.ATTACHMENT_KEY)
        val cachable = cachable.resolve(exchange)
        //we set caching headers before we try and serve from the cache
        if (cachable && cacheTime != null) {
            exchange.responseHeaders.put(Headers.CACHE_CONTROL, "public, max-age=$cacheTime")
            val date = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cacheTime!!.toLong())
            val dateHeader = DateUtils.toDateString(Date(date))
            exchange.responseHeaders.put(Headers.EXPIRES, dateHeader)
        }
        if (cache != null && cachable) {
            if (cache.tryServeResponse()) {
                return
            }
        }
        //we now dispatch to a worker thread
//as resource manager methods are potentially blocking
        val dispatchTask: HttpHandler = object : HttpHandler {
            @Throws(Exception::class)
            override fun handleRequest(exchange: HttpServerExchange) {
                var resource: Resource? = null
                try {
                    if (File.separatorChar == '/' || !exchange.relativePath.contains(File.separator)) { //we don't process resources that contain the sperator character if this is not /
//this prevents attacks where people use windows path seperators in file URLS's
                        resource = resourceSupplier!!.getResource(exchange, canonicalize(exchange.relativePath))
                    }
                } catch (e: IOException) {
                    clearCacheHeaders(exchange)
                    UndertowLogger.REQUEST_IO_LOGGER.ioException(e)
                    exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
                    exchange.endExchange()
                    return
                }
                if (resource == null) {
                    clearCacheHeaders(exchange)
                    //usually a 404 handler
                    next.handleRequest(exchange)
                    return
                }
                if (resource.isDirectory) {
                    val indexResource: Resource?
                    try {
                        indexResource = getIndexFiles(exchange, resourceSupplier, resource.path, welcomeFiles)
                    } catch (e: IOException) {
                        UndertowLogger.REQUEST_IO_LOGGER.ioException(e)
                        exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
                        exchange.endExchange()
                        return
                    }
                    if (indexResource == null) {
                        if (isDirectoryListingEnabled) {
                            DirectoryUtils.renderDirectoryListing(exchange, resource)
                            return
                        } else {
                            exchange.statusCode = StatusCodes.FORBIDDEN
                            exchange.endExchange()
                            return
                        }
                    } else if (!exchange.requestPath.endsWith("/")) {
                        exchange.statusCode = StatusCodes.FOUND
                        exchange.responseHeaders.put(
                            Headers.LOCATION,
                            RedirectBuilder.redirect(exchange, exchange.relativePath + "/", true)
                        )
                        exchange.endExchange()
                        return
                    }
                    resource = indexResource
                } else if (exchange.relativePath.endsWith("/")) { //UNDERTOW-432
                    exchange.statusCode = StatusCodes.NOT_FOUND
                    exchange.endExchange()
                    return
                }
                //Start of custom template processor
                val extension: String = FilenameUtils.getExtension(resource.path)
                val templateProcessor = TemplateProcessorFactory.get(extension)
                if (templateProcessor != null) {
                    val model = HashMap<String, Any>()
                    val output = templateProcessor.render(resource.filePath, model)
                    exchange.responseHeaders.put(Headers.CONTENT_TYPE, templateProcessor.contentType())
                    exchange.responseSender.send(output)
                    return
                }
                //End of custom template processor
                val etag = resource.eTag
                val lastModified = resource.lastModified
                if (!ETagUtils.handleIfMatch(exchange, etag, false) ||
                    !DateUtils.handleIfUnmodifiedSince(exchange, lastModified)
                ) {
                    exchange.statusCode = StatusCodes.PRECONDITION_FAILED
                    exchange.endExchange()
                    return
                }
                if (!ETagUtils.handleIfNoneMatch(exchange, etag, true) ||
                    !DateUtils.handleIfModifiedSince(exchange, lastModified)
                ) {
                    exchange.statusCode = StatusCodes.NOT_MODIFIED
                    exchange.endExchange()
                    return
                }
                val contentEncodedResourceManager =
                    contentEncodedResourceManager
                val contentLength = resource.contentLength
                if (contentLength != null && !exchange.responseHeaders.contains(Headers.TRANSFER_ENCODING)) {
                    exchange.responseContentLength = contentLength
                }
                var rangeResponse: RangeResponseResult? = null
                var start: Long = -1
                var end: Long = -1
                if (resource is RangeAwareResource && resource.isRangeSupported && contentLength != null && contentEncodedResourceManager == null) {
                    exchange.responseHeaders.put(Headers.ACCEPT_RANGES, "bytes")
                    //TODO: figure out what to do with the content encoded resource manager
                    val range =
                        ByteRange.parse(exchange.requestHeaders.getFirst(Headers.RANGE))
                    if (range != null && range.ranges == 1 && resource.getContentLength() != null) {
                        rangeResponse = range.getResponseResult(
                            resource.getContentLength(),
                            exchange.requestHeaders.getFirst(Headers.IF_RANGE),
                            resource.getLastModified(),
                            if (resource.getETag() == null) null else resource.getETag().tag
                        )
                        if (rangeResponse != null) {
                            start = rangeResponse.start
                            end = rangeResponse.end
                            exchange.statusCode = rangeResponse.statusCode
                            exchange.responseHeaders
                                .put(Headers.CONTENT_RANGE, rangeResponse.contentRange)
                            val length = rangeResponse.contentLength
                            exchange.responseContentLength = length
                            if (rangeResponse.statusCode == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
                                return
                            }
                        }
                    }
                }
                //we are going to proceed. Set the appropriate headers
                if (!exchange.responseHeaders.contains(Headers.CONTENT_TYPE)) {
                    val contentType = resource.getContentType(mimeMappings)
                    if (contentType != null) {
                        exchange.responseHeaders.put(Headers.CONTENT_TYPE, contentType)
                    } else {
                        exchange.responseHeaders
                            .put(Headers.CONTENT_TYPE, "application/octet-stream")
                    }
                }
                if (lastModified != null) {
                    exchange.responseHeaders
                        .put(Headers.LAST_MODIFIED, resource.lastModifiedString)
                }
                if (etag != null) {
                    exchange.responseHeaders.put(Headers.ETAG, etag.toString())
                }
                if (contentEncodedResourceManager != null) {
                    try {
                        val encoded =
                            contentEncodedResourceManager.getResource(resource, exchange)
                        if (encoded != null) {
                            exchange.responseHeaders
                                .put(Headers.CONTENT_ENCODING, encoded.contentEncoding)
                            exchange.responseHeaders
                                .put(Headers.CONTENT_LENGTH, encoded.resource.contentLength)
                            encoded.resource.serve(exchange.responseSender, exchange, IoCallback.END_EXCHANGE)
                            return
                        }
                    } catch (e: IOException) { //TODO: should this be fatal
                        UndertowLogger.REQUEST_IO_LOGGER.ioException(e)
                        exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
                        exchange.endExchange()
                        return
                    }
                }
                if (!sendContent) {
                    exchange.endExchange()
                } else if (rangeResponse != null) {
                    (resource as RangeAwareResource).serveRange(
                        exchange.responseSender,
                        exchange,
                        start,
                        end,
                        IoCallback.END_EXCHANGE
                    )
                } else {
                    resource.serve(exchange.responseSender, exchange, IoCallback.END_EXCHANGE)
                }
            }
        }
        if (exchange.isInIoThread) {
            exchange.dispatch(dispatchTask)
        } else {
            dispatchTask.handleRequest(exchange)
        }
    }

    private fun clearCacheHeaders(exchange: HttpServerExchange) {
        exchange.responseHeaders.remove(Headers.CACHE_CONTROL)
        exchange.responseHeaders.remove(Headers.EXPIRES)
    }

    @Throws(IOException::class)
    private fun getIndexFiles(
        exchange: HttpServerExchange,
        resourceManager: ResourceSupplier?,
        base: String,
        possible: List<String>
    ): Resource? {
        val realBase: String
        realBase = if (base.endsWith("/")) {
            base
        } else {
            "$base/"
        }
        for (possibility in possible) {
            val index =
                resourceManager!!.getResource(exchange, canonicalize(realBase + possibility))
            if (index != null) {
                return index
            }
        }
        return null
    }

    private fun canonicalize(s: String): String {
        return if (isCanonicalizePaths) {
            CanonicalPathUtils.canonicalize(s)
        } else s
    }

    fun setDirectoryListingEnabled(directoryListingEnabled: Boolean): VirtualHostRouteHandler {
        isDirectoryListingEnabled = directoryListingEnabled
        return this
    }

    fun addWelcomeFiles(vararg files: String?): VirtualHostRouteHandler {
        welcomeFiles.addAll(Arrays.asList<String>(*files))
        return this
    }

    fun setWelcomeFiles(vararg files: String?): VirtualHostRouteHandler {
        welcomeFiles.clear()
        welcomeFiles.addAll(Arrays.asList<String>(*files))
        return this
    }

    fun setMimeMappings(mimeMappings: MimeMappings): VirtualHostRouteHandler {
        this.mimeMappings = mimeMappings
        return this
    }

    fun setCachable(cachable: Predicate): VirtualHostRouteHandler {
        this.cachable = cachable
        return this
    }

    fun setAllowed(allowed: Predicate): VirtualHostRouteHandler {
        this.allowed = allowed
        return this
    }

    fun setResourceSupplier(resourceSupplier: ResourceSupplier?): VirtualHostRouteHandler {
        this.resourceSupplier = resourceSupplier
        resourceManager = null
        return this
    }

    fun setResourceManager(resourceManager: ResourceManager?): VirtualHostRouteHandler {
        this.resourceManager = resourceManager
        resourceSupplier = DefaultResourceSupplier(resourceManager)
        return this
    }

    fun setCacheTime(cacheTime: Int?): VirtualHostRouteHandler {
        this.cacheTime = cacheTime
        return this
    }

    fun setContentEncodedResourceManager(contentEncodedResourceManager: ContentEncodedResourceManager?): VirtualHostRouteHandler {
        this.contentEncodedResourceManager = contentEncodedResourceManager
        return this
    }

    class Builder : HandlerBuilder {
        override fun name(): String {
            return "resource"
        }

        override fun parameters(): Map<String, Class<*>?> {
            val params: MutableMap<String, Class<*>?> = HashMap()
            params["location"] = String::class.java
            params["allow-listing"] = Boolean::class.javaPrimitiveType
            return params
        }

        override fun requiredParameters(): Set<String> {
            return setOf("location")
        }

        override fun defaultParameter(): String {
            return "location"
        }

        override fun build(config: Map<String, Any>): HandlerWrapper {
            return Wrapper(
                config["location"] as String?,
                (config["allow-listing"] as Boolean?)!!
            )
        }
    }

    private class Wrapper constructor(
        private val location: String?,
        private val allowDirectoryListing: Boolean
    ) :
        HandlerWrapper {
        override fun wrap(handler: HttpHandler): HttpHandler {
            val rm: ResourceManager =
                PathResourceManager(Paths.get(location), 1024)
            val resourceHandler =
                VirtualHostRouteHandler(rm)
            resourceHandler.setDirectoryListingEnabled(allowDirectoryListing)
            return resourceHandler
        }

    }
}