package com.amphi.server.services.user

import com.amphi.server.common.InvalidPasswordException
import com.amphi.server.common.UnknownUserException
import com.amphi.server.configs.ServerSqliteDatabase.pool
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.vertx.core.Future
import io.vertx.sqlclient.Tuple
import java.security.SecureRandom
import java.time.Instant

class UserSqliteService : UserService {

    override fun getUserIds(): Future<Set<String>> {
        return pool.query("SELECT id FROM users").execute().map {
            it.map { row ->
                row.getString("id")
            }.toSet()
        }
    }

    override fun logout(token: String) : Future<Unit> {
      return pool.preparedQuery("DELETE FROM tokens WHERE token = ?").execute(Tuple.of(token)).mapEmpty()
    }

    override fun login(
        id: String,
        deviceName: String,
        password: String
    ) : Future<String> {
      return pool
          .preparedQuery("SELECT name, password FROM users WHERE id = ?")
        .execute(Tuple.of(id))
        .compose { rows ->
          val row = rows.firstOrNull()
          if(row != null) {
            val storedHashedPassword = row.getString("password")
            val argon2: Argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

              val authenticated = argon2.verify(storedHashedPassword, password.toCharArray())
              argon2.wipeArray(password.toCharArray())
              if(authenticated) {
                  val tokenValue = generatedToken().await()
                  val timestamp = Instant.now()
                  pool.preparedQuery("INSERT INTO tokens (token, last_accessed, user_id, device_name) VALUES(?, ?, ?, ?)")
                      .execute(Tuple.of(tokenValue, timestamp.toEpochMilli(), id, deviceName))
                      .map {
                          tokenValue
                      }
              }
              else {
                  Future.failedFuture(InvalidPasswordException())
              }
          }
          else {
            Future.failedFuture(UnknownUserException())
          }
      }
    }

    override fun generatedToken(): Future<String> {
        val random = SecureRandom()
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val stringBuilder = StringBuilder(50)

        (0 until 50).forEach { _ ->
            val randomIndex = random.nextInt(characters.length)
            stringBuilder.append(characters[randomIndex])
        }

        val token = stringBuilder.toString()

        return pool.preparedQuery("SELECT token FROM tokens WHERE token = ?")
            .execute(Tuple.of(token))
            .compose { rows ->
                if (rows.size() > 0) {
                    generatedToken()
                } else {
                    Future.succeededFuture(token)
                }
            }
    }

    override fun register(id: String, name: String, password: String) : Future<Unit> {
        val argon2: Argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

        val hashedPassword = argon2.hash(12, 65536, 4, password.toCharArray())
        argon2.wipeArray(password.toCharArray())

        return pool.preparedQuery("INSERT INTO users (id, name, password) VALUES (?, ?, ?)")
            .execute(Tuple.of(id, name, hashedPassword))
            .mapEmpty()
    }

    override fun changePassword(
        oldPassword: String,
        password: String,
        id: String
    ) : Future<Unit> {
        val rows = pool
            .preparedQuery("SELECT password FROM users WHERE id = ?")
            .execute(Tuple.of(id))
            .await()
        if(!rows.any()) {
            return Future.failedFuture(NoSuchElementException())
        }

        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        val hashedPassword = rows.first().getString("password")
        val isValid = argon2.verify(hashedPassword, oldPassword.toCharArray())
        argon2.wipeArray(oldPassword.toCharArray())
        if (!isValid) {
            return Future.failedFuture(InvalidPasswordException())
        }

        return pool.preparedQuery("UPDATE users SET password = ? WHERE id = ?").execute(Tuple.of(argon2.hash(12, 65536, 4, password.toCharArray()), id)).mapEmpty()
    }

    override fun changeUsername(name: String, id: String) : Future<Unit> {
        return pool.preparedQuery("UPDATE users SET name = ? WHERE id = ?").execute(Tuple.of(name, id)).mapEmpty()
    }
}