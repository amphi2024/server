package com.amphi.server.handlers

import io.vertx.core.http.HttpServerRequest
import com.amphi.server.common.Messages
import com.amphi.server.common.withAuth
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendSuccess
import com.amphi.server.eventService

object EventHandler {

    fun getEvents(req: HttpServerRequest, appType: String) {
        val requestToken = req.headers()["Authorization"]
        req.withAuth {
            eventService.getEvents(requestToken, appType).onSuccess { result ->
                req.response().putHeader("content-type", "application/json; charset=UTF-8")
                    .end(result.joinToString(","))
            }
                .onFailure {
                    req.response().setStatusCode(500).end(Messages.ERROR)
                }
        }
    }

    fun acknowledgeEvent(req: HttpServerRequest) {
        req.withAuth { token ->
            req.bodyHandler { buffer ->
                val jsonBody = buffer.toJsonObject()
                val action = jsonBody.getString("action")
                val value = jsonBody.getString("value")
                if (jsonBody == null || action == null || value == null) {
                    sendBadRequest(req)
                } else {
                    eventService.acknowledgeEvent(
                        token = token.token,
                        action = action,
                        value = value,
                    )
                    sendSuccess(req)
                }
            }
        }
    }
}