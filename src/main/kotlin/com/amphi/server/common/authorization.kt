package com.amphi.server.common

import com.amphi.server.authorizationService
import com.amphi.server.logger
import com.amphi.server.models.Token
import io.vertx.core.http.HttpServerRequest
import java.time.Instant

fun handleAuthorization(
    req: HttpServerRequest,
    onAuthenticated: (token: Token) -> Unit
) {
    val requestToken = req.headers()["Authorization"]

    if (requestToken.isNullOrBlank()) {
        logger?.warn("[SECURITY] Authorization Missing: IP=${req.remoteAddress().hostAddress()}, Path=${req.path()}")
        sendAuthFailed(req)
    } else {
        authorizationService.authenticateByToken(
            token = requestToken,
            onFailed = {
                logger?.warn("[SECURITY] Authorization Failed: IP=${req.remoteAddress().hostAddress()}, Path=${req.path()}")
                sendAuthFailed(req)
            },
            onAuthenticated = onAuthenticated
        )
    }
}