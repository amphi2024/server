package com.amphi

import io.vertx.core.http.HttpServerRequest

fun handlePathCheckRequest(req: HttpServerRequest, index: Int, handleRequest: (string: String) -> Unit) {
    val split = req.path().split("/")
    if(split.size > index) {
        handleRequest(split[index])
    }
    else {
        sendBadRequest(req)
    }
}