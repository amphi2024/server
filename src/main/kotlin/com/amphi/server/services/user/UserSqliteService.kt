package com.amphi.server.services.user

import com.amphi.server.configs.ServerSqliteDatabase.connection
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.vertx.core.json.JsonArray
import java.security.SecureRandom
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant

class UserSqliteService : UserService {

  override fun getUserIds(): JsonArray {
    val jsonArray = JsonArray()
    val statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery("SELECT id From users")
    while (resultSet.next()) {
      jsonArray.add(resultSet.getString("id"))
    }

    resultSet.close()
    statement.close()

    return jsonArray
  }

  override fun logout(token: String) {
    val sql = "DELETE FROM tokens WHERE token = ?;"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, token)
    preparedStatement.executeUpdate()
    preparedStatement.close()
  }


  override fun login(id: String, deviceName: String, password: String, onAuthenticated: (Boolean, String?, String?) -> Unit) {
    val sql = "SELECT name, password FROM users WHERE id = ?;"
    val statement = connection.prepareStatement(sql)

    statement.setString(1, id)

    val resultSet: ResultSet = statement.executeQuery()

    var authenticated = false
    var token: String? = null
    var username: String? = null

    if (resultSet.next()) {
      val storedHashedPassword = resultSet.getString("password")
      val argon2: Argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)


      token = generatedToken()
      username = resultSet.getString("name")
      val insertQuery = "INSERT INTO tokens (token, last_accessed, user_id, device_name) VALUES (? , ?, ?, ?);"
      val insertStatement = connection.prepareStatement(insertQuery)
      insertStatement.setString(1, token)
      insertStatement.setLong(2, Instant.now().toEpochMilli())
      insertStatement.setString(3, id)
      insertStatement.setString(4, deviceName)
      insertStatement.executeUpdate()
      insertStatement.close()
      authenticated = argon2.verify(storedHashedPassword, password.toCharArray())

      argon2.wipeArray(password.toCharArray())
    }
    resultSet.close()
    statement.close()

    onAuthenticated(authenticated, token, username)
  }

  override fun generatedToken(): String {
    val random = SecureRandom()
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val stringBuilder = StringBuilder(50)
    for (i in 0 until 50) {
      val randomIndex = random.nextInt(characters.length)
      stringBuilder.append(characters[randomIndex])
    }
    val string = stringBuilder.toString()
    var exists = false

//    for(token in tokens) {
//      if(string == token.token) {
//        exists = true
//        break
//      }
//    }
    return if(exists) {
      generatedToken()
    } else string
  }

  override fun register(id: String, name: String, password: String, onFailed: () -> Unit, onSuccess: () -> Unit) {
    try {
      val argon2: Argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

      val hashedPassword = argon2.hash(12, 65536, 4, password.toCharArray())

      val sql = "INSERT INTO users (id, name, password) VALUES ( ? , ? , ?);"
      val preparedStatement = connection.prepareStatement(sql)

      preparedStatement.setString(1, id)
      preparedStatement.setString(2, name)
      preparedStatement.setString(3, hashedPassword)
      preparedStatement.executeUpdate()
      preparedStatement.close()
      argon2.wipeArray(password.toCharArray())
      onSuccess()
    } catch (_: SQLException) {
      onFailed()
    }
  }

  override fun changePassword(
    oldPassword: String,
    password: String,
    id: String,
    onAuthenticationFailed: () -> Unit,
    onSuccess: () -> Unit
  ) {
    val sql = "SELECT password FROM users WHERE id = ?;"
    val statement = connection.prepareStatement(sql)
    statement.setString(1, id)
    val resultSet: ResultSet = statement.executeQuery()
    var authenticated = false
    val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
    if(resultSet.next()) {
      val hashedPassword = resultSet.getString("password")
      authenticated = argon2.verify(hashedPassword, oldPassword.toCharArray())
      argon2.wipeArray(oldPassword.toCharArray())
    }

    statement.close()
    resultSet.close()

    if(authenticated) {
      val updateSQL = "UPDATE users SET password = ? WHERE id = ?;"
      val preparedStatement = connection.prepareStatement(updateSQL)
      val hashedPassword = argon2.hash(12, 65536, 4, password.toCharArray())
      preparedStatement.setString(1, hashedPassword)
      preparedStatement.setString(2, id)
      preparedStatement.executeUpdate()
      preparedStatement.close()
      argon2.wipeArray(password.toCharArray())
      onSuccess()
    }
    else {
      onAuthenticationFailed()
    }
  }

  override fun changeUsername(name: String, id: String) {
    val sql = "UPDATE users SET name = ? WHERE id = ?;"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, name)
    preparedStatement.setString(2, id)
    preparedStatement.executeUpdate()
    preparedStatement.close()
  }
}
