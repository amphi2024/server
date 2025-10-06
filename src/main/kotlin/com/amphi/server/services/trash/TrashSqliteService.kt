package com.amphi.server.services.trash

import com.amphi.server.configs.ServerSqliteDatabase.connection
import com.amphi.server.models.TrashLog
import java.sql.ResultSet
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

    override fun getTrashLogs(): List<TrashLog> {
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

    override fun deleteTrashLog(path: String) {
        val sql = "DELETE FROM trashes WHERE path = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, path)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }
}
