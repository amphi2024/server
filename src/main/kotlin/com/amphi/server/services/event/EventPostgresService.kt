package com.amphi.server.services.event

import com.amphi.server.models.Token
import io.vertx.core.json.JsonArray

class EventPostgresService : EventService {
  override fun getEvents(token: String, appType: String): JsonArray {
    TODO("Not yet implemented")
  }

  override fun saveEvent(
    token: Token,
    action: String,
    value: String,
    appType: String?
  ) {
    TODO("Not yet implemented")
  }

  override fun acknowledgeEvent(token: String, action: String, value: String) {
    TODO("Not yet implemented")
  }

    override fun notifyNotesMigration(userId: String, idChanges: MutableMap<String, String>) {
        TODO("Not yet implemented")
    }
}
