package com.amphi.server.services.event

import com.amphi.server.models.Token
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

interface EventService {
    fun getEvents(token: String, appType: String) : Future<Set<JsonObject>>
    fun saveEvent(token: Token, action: String, value: String, appType: String?) : Future<Unit>
    fun acknowledgeEvent(token: String, action: String, value: String) : Future<Unit>
}