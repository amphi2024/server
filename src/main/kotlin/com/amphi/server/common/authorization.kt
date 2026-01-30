package com.amphi.server.common

import com.amphi.server.authorizationService
import com.amphi.server.models.Token
import io.vertx.core.http.HttpServerRequest
import java.time.Instant

fun handleAuthorization(
    req: HttpServerRequest,
    onAuthenticated: (token: Token) -> Unit
) {
    val requestToken = req.headers()["Authorization"]

    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        authorizationService.authenticateByToken(
            token = requestToken,
            onFailed = { sendAuthFailed(req) },
            onAuthenticated = onAuthenticated
        )
    }
}