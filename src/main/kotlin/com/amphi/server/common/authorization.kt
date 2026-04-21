package com.amphi.server.common

import com.amphi.server.authorizationService
import com.amphi.server.logger
import com.amphi.server.models.Token
import io.vertx.core.http.HttpServerRequest

fun HttpServerRequest.withAuth(onAuthenticated: (token: Token) -> Unit) {
    val requestToken = this.headers()["Authorization"]

    if (requestToken.isNullOrBlank()) {
        logger?.warn("[SECURITY] Authorization Missing: IP=${this.remoteAddress().hostAddress()}")
        sendAuthFailed(this)
        return
    }

    authorizationService.authenticateByToken(
        token = requestToken,
    ).onSuccess {
      onAuthenticated(it)
    }.onFailure {
        logger?.warn("[SECURITY] Authorization Failed: IP=${this.remoteAddress().hostAddress()}, Path=${this.path()}")
        sendAuthFailed(this)
    }
}