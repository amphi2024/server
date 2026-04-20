package com.amphi.server.services.event

import com.amphi.server.authorizationService
import com.amphi.server.configs.ServerSqliteDatabase.pool
import com.amphi.server.models.Token
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Tuple
import java.time.Instant

class EventSqliteService : EventService {
    override fun getEvents(token: String, appType: String): Future<Set<JsonObject>> {
        return pool.preparedQuery("SELECT action, value, timestamp FROM events WHERE token = ? AND (app_type = ? OR app_type IS NULL)")
            .execute(Tuple.of(token, appType))
            .map { rows ->
                rows.map {
                    val jsonObject = JsonObject()
                    jsonObject.put("action", it.getString("action"))
                    jsonObject.put("value", it.getString("value"))
                    jsonObject.put("timestamp", it.getLong("timestamp"))
                    jsonObject
                }.toSet()
            }
    }

    override fun saveEvent(
        token: Token,
        action: String,
        value: String,
        appType: String?
    ): Future<Unit> {
        val instantValue = Instant.now().toEpochMilli()

        return authorizationService.getTokens().compose { tokens ->
            val tasks: List<Future<Unit>> = tokens
                .filter { item -> item.userId == token.userId && item.token != token.token }
                .map { item ->
                    pool.preparedQuery("INSERT INTO events (token, action, value, timestamp, app_type) VALUES (?, ?, ?, ?, ?)")
                        .execute(Tuple.of(item.token, action, value, instantValue, appType)).mapEmpty()
                }

            if (tasks.isEmpty()) {
                Future.succeededFuture()
            } else {
                Future.all<List<Future<Unit>>>(tasks).mapEmpty()
            }
        }
    }

    override fun acknowledgeEvent(token: String, action: String, value: String): Future<Unit> {
        return pool.preparedQuery("DELETE FROM events WHERE token = ? AND action = ? AND value = ?")
            .execute(Tuple.of(token, action, value)).mapEmpty()
    }
}


