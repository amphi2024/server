package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.models.ConnectedUser
import com.amphi.server.common.handleAuthorization
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpServerRequest

class WebsocketHandler {
    private val connectedUsers: MutableList<ConnectedUser> = mutableListOf()

    fun handleWebsocket(req: HttpServerRequest) {
        handleAuthorization(req) { token ->
            req.toWebSocket().onComplete { asyncResult ->
                if (asyncResult.succeeded()) {
                    val ws = asyncResult.result()

                    connectedUsers.add(
                        ConnectedUser(
                            webSocket = ws,
                            token = token
                        )
                    )

                    ws.handler { message ->
                        for (user in connectedUsers) {
                            if (user.token.token != token.token && user.token.userId == token.userId) {
                                user.webSocket.writeTextMessage(message.toString())
                            }
                        }

                    }

                } else {
                    req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(Messages.FAILED)
                }
            }
        }
    }
}