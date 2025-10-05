package com.amphi.server.services.event

import com.amphi.server.models.Token
import io.vertx.core.json.JsonArray

interface EventService {
  fun getEvents(token: String, appType: String): JsonArray
  fun saveEvent(token: Token, action: String, value: String, appType: String?)
  fun acknowledgeEvent(token: String, action: String, value: String)
}
