package com.amphi.server.handlers

import io.vertx.core.http.HttpServerRequest
import com.amphi.server.authorizationService
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendSuccess
import com.amphi.server.eventService

object EventHandler {

    fun getEvents(req: HttpServerRequest, appType: String) {
        val requestToken = req.headers()["Authorization"]
        handleAuthorization(req) {
            authorizationService.syncTokensLastAccess()
            val jsonArray = eventService.getEvents(requestToken, appType)
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun acknowledgeEvent(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val jsonBody = buffer.toJsonObject()
                val action = jsonBody.getString("action")
                val value = jsonBody.getString("value")
                if (jsonBody == null || action == null || value == null) {
                    sendBadRequest(req)
                } else {
                    //println("event is acknowledged  $action, $value, $requestToken")
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