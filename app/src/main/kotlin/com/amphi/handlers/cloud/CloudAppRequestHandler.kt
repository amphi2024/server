package com.amphi.handlers.cloud

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object CloudAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {
        when {
            else -> sendNotFound(req)
        }
    }
}