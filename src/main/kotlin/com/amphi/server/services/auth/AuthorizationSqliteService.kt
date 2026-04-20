package com.amphi.server.services.auth

import com.amphi.server.configs.AppConfig
import com.amphi.server.configs.ServerSqliteDatabase.pool
import com.amphi.server.models.Token
import io.vertx.core.Future
import io.vertx.sqlclient.Tuple
import java.time.Duration
import java.time.Instant

class AuthorizationSqliteService : AuthorizationService {

    override fun authenticateByToken(token: String) : Future<Token> {
        return pool
            .preparedQuery("SELECT user_id, device_name FROM tokens WHERE token = ?")
            .execute(Tuple.of(token))
            .compose { rows ->
                val row = rows.firstOrNull()
                val instant = Instant.now()
                if (row != null) {
                    val userToken = Token(
                        token = token,
                        userId = row.getString("user_id"),
                        lastAccessed = instant,
                        deviceName = row.getString("device_name")
                    )

                    pool.preparedQuery("UPDATE tokens SET last_accessed = ? WHERE token = ?")
                        .execute(Tuple.of(instant.toEpochMilli(), token))
                        .await()
                    Future.succeededFuture(userToken)
                } else {
                    Future.failedFuture(SecurityException())
                }
            }
    }

    override fun deleteObsoleteTokens() : Future<Unit> {

        val expirationThreshold = Instant.now().minus(Duration.ofDays(AppConfig.loginExpirationDays.toLong()))

        return pool.preparedQuery("DELETE FROM tokens WHERE last_accessed < ?")
            .execute(Tuple.of(expirationThreshold))
            .compose {
                pool.preparedQuery("""
            DELETE FROM events 
            WHERE NOT EXISTS (
                SELECT 1 FROM tokens WHERE tokens.token = events.token
            )
        """.trimIndent()).execute()
            }
            .mapEmpty()
    }

    override fun getTokens(): Future<Set<Token>> {
        return pool.query("SELECT * FROM tokens").execute().map {
            it.map { row ->
                Token(
                    userId = row.getString("user_id"),
                    token = row.getString("token"),
                    lastAccessed = Instant.ofEpochMilli(row.getLong("last_accessed")),
                    deviceName = row.getString("device_name")
                )
            }.toSet()
        }
    }
}
