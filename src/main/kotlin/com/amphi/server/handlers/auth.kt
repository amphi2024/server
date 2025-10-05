package com.amphi.server.handlers

import com.amphi.server.ServerDatabase
import com.amphi.server.models.Token
import com.amphi.server.sendAuthFailed
import io.vertx.core.http.HttpServerRequest

fun handleAuth(
    req: HttpServerRequest,
    onAuthenticated: (token: Token) -> Unit
) {
    val requestToken = req.headers()["Authorization"]

    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = { sendAuthFailed(req) },
            onAuthenticated = onAuthenticated
        )
    }
}