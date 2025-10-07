package com.amphi.server.configs
import java.sql.Connection
import java.sql.DriverManager

object ServerSqliteDatabase {

  val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:sqlite:database.db")
  }

  fun close() {
    connection.close()
  }

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
            """.trimIndent()
        )
        statement.executeUpdate(
          """
                CREATE TABLE IF NOT EXISTS tokens (
                    token TEXT PRIMARY KEY NOT NULL,
                    last_accessed INTEGER NOT NULL,
                    user_id TEXT NOT NULL,
                    device_name TEXT NOT NULL
                );
            """.trimIndent()
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
            """.trimIndent()
        )
          val tableExists = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='trashes';").next()

          if (tableExists) {
              statement.executeUpdate("ALTER TABLE trashes RENAME TO trash;")
          }
        statement.executeUpdate(
          """
           CREATE TABLE IF NOT EXISTS trash (
                    path TEXT PRIMARY KEY NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """.trimIndent()
        )
      }
    } catch (e: Exception) {
      println(e.message)
    }
  }

}
