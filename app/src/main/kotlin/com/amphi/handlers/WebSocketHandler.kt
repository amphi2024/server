package com.amphi.handlers

import io.vertx.core.http.HttpServerRequest
import com.amphi.models.ConnectedUser
import com.amphi.Messages
import com.amphi.ServerDatabase
import com.amphi.StatusCode
import com.amphi.sendAuthFailed

object WebSocketHandler {

    private val connectedNotesAppUsers: MutableList<ConnectedUser> = mutableListOf()

    fun handleNotesAppWebsocket(req: HttpServerRequest) {
        val token: String = req.headers()["Authorization"].toString()
        ServerDatabase.authenticateByToken(
            token = token,
            onAuthenticated = { authenticatedToken ->
                req.toWebSocket { asyncResult ->

                    if (asyncResult.succeeded()) {
                        val ws = asyncResult.result()

                        connectedNotesAppUsers.add(
                            ConnectedUser(
                                webSocket = ws,
                                token = authenticatedToken
                            )
                        )

                        ws.handler { message ->
                            for (user in connectedNotesAppUsers) {
                                if (user.token.token != authenticatedToken.token && user.token.userId == authenticatedToken.userId) {
                                    println(true)
                                    user.webSocket.writeTextMessage(message.toString())
                                }
                            }

                        }

                            ws.closeHandler {
                                println("WebSocket closed: ${req.remoteAddress()}")
                            }

//                            ws.exceptionHandler { throwable ->
//                                println("WebSocket error: ${throwable.message}")
//                            }
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