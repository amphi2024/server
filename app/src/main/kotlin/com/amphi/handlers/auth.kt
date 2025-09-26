package com.amphi.handlers

import com.amphi.ServerDatabase
import com.amphi.models.Token
import com.amphi.sendAuthFailed
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