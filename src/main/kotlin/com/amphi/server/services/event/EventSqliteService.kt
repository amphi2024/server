package com.amphi.server.services.event

import com.amphi.server.authorizationService
import com.amphi.server.configs.ServerSqliteDatabase.connection
import com.amphi.server.models.Token
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.sql.ResultSet
import java.time.Instant

class EventSqliteService : EventService {
    override fun getEvents(token: String, appType: String): JsonArray {
        val jsonArray = JsonArray()
        val sql = "SELECT action, value, timestamp FROM events WHERE token = ? AND (app_type = ? OR app_type IS NULL);"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, token)
        statement.setString(2, appType)
        val resultSet: ResultSet = statement.executeQuery()
        while (resultSet.next()) {
            val jsonObject = JsonObject()
            jsonObject.put("action", resultSet.getString("action"))
            jsonObject.put("value", resultSet.getString("value"))
            jsonObject.put("timestamp", resultSet.getLong("timestamp"))
            jsonArray.add(jsonObject)
        }

        resultSet.close()
        statement.close()

        return jsonArray
    }

    override fun saveEvent(
        token: Token,
        action: String,
        value: String,
        appType: String?
    ) {
        for (item in authorizationService.getTokens()) {
            if (item.userId == token.userId && item.token != token.token) {
                //println("saved event ${item.userId}, ${item.token}, ${item.deviceName}, $action, $appType")
                val sql = "INSERT INTO events (token, action, value, timestamp, app_type) VALUES ( ? , ? , ?, ?, ? );"
                val preparedStatement = connection.prepareStatement(sql)
                preparedStatement.setString(1, item.token)
                preparedStatement.setString(2, action)
                preparedStatement.setString(3, value)
                preparedStatement.setLong(4, Instant.now().toEpochMilli())
                preparedStatement.setString(5, appType)
                preparedStatement.executeUpdate()
                preparedStatement.close()
            }
        }
    }

    override fun acknowledgeEvent(token: String, action: String, value: String) {
        val sql = "DELETE FROM events WHERE token = ? AND action = ? AND value = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, token)
        preparedStatement.setString(2, action)
        preparedStatement.setString(3, value)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }
}


