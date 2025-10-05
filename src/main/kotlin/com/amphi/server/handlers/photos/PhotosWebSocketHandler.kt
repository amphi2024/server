package com.amphi.server.handlers.photos

import com.amphi.server.Messages
import com.amphi.server.ServerDatabase
import com.amphi.server.StatusCode
import com.amphi.server.models.ConnectedUser
import com.amphi.server.sendAuthFailed
import io.vertx.core.http.HttpServerRequest

object PhotosWebSocketHandler {

    private val connectedAppUsers: MutableList<ConnectedUser> = mutableListOf()

    fun handleWebsocket(req: HttpServerRequest) {
        val token: String = req.headers()["Authorization"].toString()
        ServerDatabase.authenticateByToken(
            token = token,
            onAuthenticated = { authenticatedToken ->
                req.toWebSocket().onComplete { asyncResult ->
                    if (asyncResult.succeeded()) {
                        val ws = asyncResult.result()

                        connectedAppUsers.add(
                            ConnectedUser(
                                webSocket = ws,
                                token = authenticatedToken
                            )
                        )

                        ws.handler { message ->
                            for (user in connectedAppUsers) {
                                if (user.token.token != authenticatedToken.token && user.token.userId == authenticatedToken.userId) {
                                    user.webSocket.writeTextMessage(message.toString())
                                }
                            }

                        }

                    } else {
                        req.response().setStatusCode(StatusCode.INTERNAL_SERVER_ERROR).end(Messages.FAILED)
                    }

                }
            },
            onFailed = {
                sendAuthFailed(req)
            })
    }

}