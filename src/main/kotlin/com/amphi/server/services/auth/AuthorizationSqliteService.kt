package com.amphi.server.services.auth

import com.amphi.server.configs.ServerSettings
import com.amphi.server.configs.ServerSqliteDatabase.connection
import com.amphi.server.models.Token
import java.time.Duration
import java.time.Instant

class AuthorizationSqliteService : AuthorizationService {

    private val tokens : MutableList<Token> = mutableListOf()

    init {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT token, last_accessed, user_id, device_name FROM tokens").use { resultSet ->
                while (resultSet.next()) {
                    tokens.add(
                        Token(
                            userId = resultSet.getString("user_id"),
                            token = resultSet.getString("token"),
                            lastAccessed = Instant.ofEpochMilli(resultSet.getLong("last_accessed")),
                            deviceName = resultSet.getString("device_name")
                        )
                    )
                }
            }
        }
    }

  override fun authenticateByToken(token: String, onAuthenticated: (Token) -> Unit, onFailed: () -> Unit) {
      var authenticated = false
      for (item in tokens) {
          if(token == item.token) {
              item.lastAccessed = Instant.now()
              authenticated = true
              onAuthenticated(item)
              break
          }
      }
      if(!authenticated) {
          onFailed()
      }


//    connection.prepareStatement("SELECT user_id, device_name FROM tokens WHERE token = ?").use { statement ->
//      statement.setString(1, token)
//      val resultSet = statement.executeQuery()
//      if(resultSet.next()) {
//        val instant = Instant.now()
//        onAuthenticated(
//          Token(
//            token = token,
//            userId = resultSet.getString("user_id"),
//            lastAccessed = instant,
//            deviceName = resultSet.getString("device_name")
//          )
//        )
//        connection.prepareStatement("UPDATE tokens SET last_accessed = ? WHERE token = ?;").use { updateStmt ->
//          updateStmt.setLong(1, instant.toEpochMilli())
//          updateStmt.setString(2, token)
//          updateStmt.execute()
//        }
//      }
//      else {
//        onFailed()
//      }
//      resultSet.close()
//    }
  }

  override fun deleteObsoleteTokens() {
      val statement = connection.createStatement()
      val days = Duration.ofDays(ServerSettings.loginExpirationPeriod.toLong())
      val fewDayAgo = Instant.now().minus(days)
      val iterator = tokens.iterator()
      while (iterator.hasNext()) {
          val token = iterator.next()
          if (token.lastAccessed.isBefore(fewDayAgo)) {
              iterator.remove()
              val deleteStatement = connection.prepareStatement("DELETE FROM tokens WHERE token = ?")

              deleteStatement.setString(1, token.token)
              deleteStatement.executeUpdate()
              deleteStatement.close()
          }
      }

      val resultSet = statement.executeQuery("SELECT token FROM events")
      val tokensToDelete = mutableListOf<String>()

      while (resultSet.next()) {
          val eventToken = resultSet.getString(("token"))
          var exists = false
          for(token in tokens) {
              if(token.token == eventToken) {
                  exists = true
                  break
              }
          }
          if(!exists) {
              tokensToDelete.add(eventToken)
          }
      }
      resultSet.close()

      tokensToDelete.forEach { tokenString ->
          val deleteStatement = connection.prepareStatement("DELETE FROM events WHERE token = ?")
          deleteStatement.setString(1, tokenString)
          deleteStatement.executeUpdate()
          deleteStatement.close()
      }

      statement.close()
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

    override fun syncTokensLastAccess() {
        tokens.forEach { token ->
            val sql = "UPDATE tokens SET last_accessed = ? WHERE token = ?;"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setLong(1, token.lastAccessed.toEpochMilli())
            preparedStatement.setString(2, token.token)
            preparedStatement.executeUpdate()
            preparedStatement.close()
        }
    }
}
