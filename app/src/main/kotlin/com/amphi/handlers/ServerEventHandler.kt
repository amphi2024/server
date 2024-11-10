package com.amphi.handlers

import io.vertx.core.http.HttpServerRequest
import com.amphi.Messages
import com.amphi.ServerDatabase
import com.amphi.sendAuthFailed
import com.amphi.sendBadRequest

object ServerEventHandler {

    fun handleGetEvents(req:HttpServerRequest, appType: String) {
        val requestToken = req.headers()["Authorization"]
        if (requestToken.isNullOrBlank()) {
             sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                     sendAuthFailed(req)
                },
                onAuthenticated = {
                    ServerDatabase.syncTokensLastAccess()
                    val jsonArray = ServerDatabase.getEvents(requestToken, appType)
                    req.response().putHeader("content-type", "application/json").end(jsonArray.encode())
                }
            )

        }
    }

    fun handleAcknowledgeEvent(req:HttpServerRequest) {
        req.bodyHandler { buffer ->
            val jsonBody = buffer.toJsonObject()
            if (jsonBody == null) {
                 sendAuthFailed(req)
            } else {
                val requestToken = req.headers()["Authorization"]
                val action = jsonBody.getString("action")
                val value = jsonBody.getString("value")

                if (requestToken.isNullOrBlank() || action.isNullOrBlank() || value.isNullOrBlank()) {
                     sendBadRequest(req)
                } else {
                    ServerDatabase.authenticateByToken(
                        token = requestToken,
                        onFailed = {
                             sendAuthFailed(req)
                        },
                        onAuthenticated = {
                            println("event is acknowledged  $action, $value, $requestToken")
                            ServerDatabase.acknowledgeEvent(
                                token = requestToken,
                                action = action,
                                value = value,
                            )
                            req.response().putHeader("content-type", "text/plain").end(Messages.SUCCESS)
                        }
                    )

                }
            }
        }
    }
}