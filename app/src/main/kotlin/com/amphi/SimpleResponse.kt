package com.amphi

import io.vertx.core.http.HttpServerRequest

fun sendAuthFailed(req: HttpServerRequest) {
    req.response().putHeader("content-type", "text/plain").setStatusCode(StatusCode.UNAUTHORIZED).end(Messages.AUTHENTICATION_FAILED)
}

fun sendNotFound(req: HttpServerRequest) {
    req.response().putHeader("content-type", "text/plain").setStatusCode(StatusCode.NOT_FOUND).end(Messages.NOT_FOUND)
}

fun sendSuccess(req: HttpServerRequest) {
    req.response().putHeader("content-type", "text/plain").setStatusCode(StatusCode.SUCCESS).end(Messages.SUCCESS)
}

fun sendBadRequest(req: HttpServerRequest) {
    req.response().putHeader("content-type", "text/plain").setStatusCode(StatusCode.BAD_REQUEST).end(Messages.BAD_REQUEST)
}

fun sendUploadFailed(req: HttpServerRequest) {
    req.response().setStatusCode(StatusCode.INTERNAL_SERVER_ERROR).end(Messages.UPLOAD_FAILED)
}

fun sendFileNotExists(req: HttpServerRequest) {
    req.response().setStatusCode(StatusCode.NOT_FOUND).end(Messages.FILE_NOT_EXISTS)
}