package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.models.ConnectedUser
import com.amphi.server.common.withAuth
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpServerRequest

class WebsocketHandler {
    private val connectedUsers: MutableSet<ConnectedUser> = mutableSetOf()

    fun handleWebsocket(req: HttpServerRequest) {
        req.withAuth { token ->
            req.toWebSocket().onComplete { asyncResult ->
                if (asyncResult.succeeded()) {
                    val ws = asyncResult.result()

                    val currentUser = ConnectedUser(webSocket = ws, token = token)

                    connectedUsers.add(currentUser)

                    ws.closeHandler {
                        connectedUsers.remove(currentUser)
                    }

                    ws.exceptionHandler {
                        connectedUsers.remove(currentUser)
                        ws.close()
                    }

                    ws.handler { message ->
                        connectedUsers.filter { user ->
                            user.token.token != token.token && user.token.userId == token.userId
                        }.forEach { user ->
                            user.webSocket.writeTextMessage(message.toString())
                        }
                    }

                } else {
                    req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(Messages.FAILED)
                }
            }
        }
    }
}