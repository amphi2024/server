package com.amphi.handlers

import io.vertx.core.http.HttpServerRequest

object DriveAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {
        when {
            req.path().startsWith("/drive/files") -> {

            }
            req.path().startsWith("/drive/directories") -> {

            }
            req.path().startsWith("/drive/move") -> {

            }
            req.path().startsWith("/drive/copy") -> {

            }
        }
    }
}