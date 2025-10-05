package com.amphi.server.services.trash

import com.amphi.server.configs.ServerSqliteDatabase.connection
import java.time.Instant

class TrashSqliteService : TrashService {
  override fun notifyFileDelete(filePath: String) {
    val sql = "INSERT INTO trashes (path, timestamp) VALUES ( ? , ?);"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, filePath)
    preparedStatement.setLong(2, Instant.now().toEpochMilli())
    preparedStatement.executeUpdate()
    preparedStatement.close()
  }
}
