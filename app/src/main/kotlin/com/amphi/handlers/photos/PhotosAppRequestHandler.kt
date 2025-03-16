package com.amphi.handlers.photos

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object PhotosAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {
        when {
            else -> sendNotFound(req)
        }
    }
}