package com.amphi.server.services.auth

import com.amphi.server.configs.ServerSqliteDatabase.connection
import com.amphi.server.models.Token
import java.time.Instant

class AuthorizationSqliteService : AuthorizationService {
  override fun authenticateByToken(token: String, onAuthenticated: (Token) -> Unit, onFailed: () -> Unit) {
    connection.prepareStatement("SELECT user_id, device_name FROM tokens WHERE token = ?").use { statement ->
      statement.setString(1, token)
      val resultSet = statement.executeQuery()
      if(resultSet.next()) {
        val instant = Instant.now()
        onAuthenticated(
          Token(
            token = token,
            userId = resultSet.getString("user_id"),
            lastAccessed = instant,
            deviceName = resultSet.getString("device_name")
          )
        )
        connection.prepareStatement("UPDATE tokens SET last_accessed = ? WHERE token = ?;").use { updateStmt ->
          updateStmt.setLong(1, instant.toEpochMilli())
          updateStmt.setString(2, token)
          updateStmt.execute()
        }
      }
      else {
        onFailed()
      }
      resultSet.close()
    }
  }

  override fun deleteObsoleteTokens() {
    TODO("Not yet implemented")
  }

  override fun generatedToken(): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    while (true) {
      val id = (1..60)
        .map { chars.random() }
        .joinToString("")

      val sql = "SELECT COUNT(*) FROM tokens WHERE token = ?"
      connection.prepareStatement(sql).use { stmt ->
        stmt.setString(1, id)
        val rs = stmt.executeQuery()
        if (rs.next() && rs.getInt(1) == 0) {
          return id
        }
      }
    }
  }
}
