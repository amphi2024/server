package com.amphi.server.configs
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCConnectOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

object ServerSqliteDatabase {

    lateinit var pool: Pool

    fun init(vertx: Vertx) {
        val connectOptions = JDBCConnectOptions()
            .setJdbcUrl("jdbc:sqlite:database.db")
        val poolOptions= PoolOptions().setMaxSize(16)

        pool = JDBCPool.pool(vertx, connectOptions, poolOptions)
        pool.query(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                password TEXT NOT NULL
            );
            """.trimIndent()
        ).execute().await()
        pool.query(
            """
                CREATE TABLE IF NOT EXISTS tokens (
                    token TEXT PRIMARY KEY NOT NULL,
                    last_accessed INTEGER NOT NULL,
                    user_id TEXT NOT NULL,
                    device_name TEXT NOT NULL
                );
            """.trimIndent()
        ).execute().await()
        pool.query(
            """
                 CREATE TABLE IF NOT EXISTS events (
                    token TEXT NOT NULL,
                    action TEXT NOT NULL,
                    value TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    app_type TEXT
                );
            """.trimIndent()
        ).execute().await()
        val tableExists = pool.query("SELECT name FROM sqlite_master WHERE type='table' AND name='trashes';").execute().await().any()
        if (tableExists) {
            pool.query("ALTER TABLE trashes RENAME TO trash;").execute().await()
        }
        pool.query(
            """
               CREATE TABLE IF NOT EXISTS trash (
                    path TEXT PRIMARY KEY NOT NULL,
                    timestamp INTEGER NOT NULL
               );
            """.trimIndent()
        ).execute().await()
    }

  fun close() {
      pool.close()
  }
}
