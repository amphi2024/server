package com.amphi.server.configs
import java.sql.DriverManager

object ServerSqliteDatabase {

  val connection by lazy {
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
    } catch (e: Exception) {
      println(e.message)
    }
  }

}
