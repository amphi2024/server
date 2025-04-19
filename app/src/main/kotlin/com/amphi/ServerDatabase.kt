package com.amphi

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import com.amphi.models.Token
import com.amphi.models.TrashLog
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import java.sql.*
import java.time.Duration
import java.time.Instant

object ServerDatabase {

    private val connection by lazy {
        DriverManager.getConnection("jdbc:sqlite:database.db")
    }

    fun close() {
        connection.close()
    }

    private val tokens : MutableList<Token> = mutableListOf()

    init {

        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
            """
                )
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS tokens (
                    token TEXT PRIMARY KEY NOT NULL,
                    last_accessed INTEGER NOT NULL,
                    user_id TEXT NOT NULL,
                    device_name TEXT NOT NULL
                );
            """
                )
                statement.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS events (
                    token TEXT NOT NULL,
                    action TEXT NOT NULL,
                    value TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    app_type TEXT
                );
            """
                )
                statement.executeUpdate(
                    """
           CREATE TABLE IF NOT EXISTS trashes (
                    path TEXT PRIMARY KEY NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """
                )
            }



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
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun registerUser(id: String, name: String, password: String, onFailed: () -> Unit) {
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
        } catch (e: SQLException) {
            onFailed()
        }
    }

    fun login(id: String, deviceName: String, password: String, onAuthenticated: (Boolean, String?, String?) -> Unit) {
        val sql = "SELECT name, password FROM users WHERE id = ?;"
        val statement = connection.prepareStatement(sql)

        // Set the user ID parameter
        statement.setString(1, id)

        // Execute the query
        val resultSet: ResultSet = statement.executeQuery()

        var authenticated = false
        var token: String? = null
        var username: String? = null

        if (resultSet.next()) {
            // Retrieve the stored password hash and username
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

            tokens.add(
                Token(
                userId = id,
                token = token,
                deviceName = deviceName,
                lastAccessed = Instant.now()
              )
            )
            argon2.wipeArray(password.toCharArray())
        }
        resultSet.close()
        statement.close()

        onAuthenticated(authenticated, token, username)
    }

    fun logout(token: String) {
        val sql = "DELETE FROM tokens WHERE token = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, token)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun renameUser(name: String, id: String) {
        val sql = "UPDATE users SET name = ? WHERE id = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, name)
        preparedStatement.setString(2, id)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun changeUserPassword(oldPassword: String, password: String, id: String, onAuthenticationFailed: () -> Unit, onSuccess: () -> Unit) {
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

        println(authenticated)

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

    /**삭제된 파일의 경로를 참조함*/
    fun notifyFileDelete(path:String) {
        val sql = "INSERT INTO trashes (path, timestamp) VALUES ( ? , ?);"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, path)
        preparedStatement.setLong(2, Instant.now().toEpochMilli())
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    /**삭제된 파일의 경로랑 삭제된 날짜 정보들 불러옴*/
    fun getTrashLogs() : List<TrashLog> {
        val list = mutableListOf<TrashLog>()
        val sql = "SELECT path, timestamp FROM trashes;"
        val statement = connection.prepareStatement(sql)
        val resultSet: ResultSet = statement.executeQuery()
        while (resultSet.next()) {
            list.add(
                TrashLog(
                path = resultSet.getString("path"),
                timeStamp = Instant.ofEpochMilli(resultSet.getLong("timestamp"))
            )
            )
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun deleteTrashLog(path: String) {
        val sql = "DELETE FROM trashes WHERE path = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, path)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun saveEvent(token: Token, action: String, value: String, appType: String?) {
        for(item in tokens) {
            if(item.userId == token.userId && item.token != token.token) {
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

    fun getEvents(token: String, appType: String): JsonArray {
        val jsonArray = JsonArray()
        val sql = "SELECT action, value, timestamp FROM events WHERE token = ? AND (app_type = ? OR app_type IS NULL);"
        val statement = connection.prepareStatement(sql)
        statement.setString(1 , token)
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

        println(jsonArray)

        return jsonArray
    }

    fun acknowledgeEvent(token: String, action: String, value: String) {
        val sql = "DELETE FROM events WHERE token = ? AND action = ? AND value = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, token)
        preparedStatement.setString(2, action)
        preparedStatement.setString(3, value)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    private fun generatedToken() : String {
        val random = SecureRandom()
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val stringBuilder = StringBuilder(50)
        for (i in 0 until 50) {
            val randomIndex = random.nextInt(characters.length)
            stringBuilder.append(characters[randomIndex])
        }
        val string = stringBuilder.toString()
        var exists = false

        for(token in tokens) {
            if(string == token.token) {
                exists = true
                break
            }
        }
        return if(exists) {
            generatedToken()
        } else string
    }

    fun authenticateByToken(token: String, onAuthenticated: (Token) -> Unit, onFailed: () -> Unit) {
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
    }

    fun getUserIds() : JsonArray {
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

    /**유효기간 자난 토큰, 토큰에 딸린 이벤트, 존재하지 않는 토큰을 참조하는 이벤트 싹다 삭제*/
    fun deleteObsoleteTokens() {
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

    /**토큰의 마지막 접근 기간 업데이트*/
    fun syncTokensLastAccess() {

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