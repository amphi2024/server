package com.amphi.server.services.event

import com.amphi.server.models.Token
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

class EventPostgresService : EventService {
  override fun getEvents(
    token: String,
    appType: String
  ): Future<Set<JsonObject>> {
    TODO("Not yet implemented")
  }

  override fun saveEvent(
    token: Token,
    action: String,
    value: String,
    appType: String?
  ): Future<Unit> {
    TODO("Not yet implemented")
  }

  override fun acknowledgeEvent(
    token: String,
    action: String,
    value: String
  ): Future<Unit> {
    TODO("Not yet implemented")
  }

}
