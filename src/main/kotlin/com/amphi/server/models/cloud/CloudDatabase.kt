package com.amphi.server.models.cloud

import com.amphi.server.configs.AppConfig
import com.amphi.server.models.FileModel
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import kotlin.random.Random

class CloudDatabase(val userId: String) {
    private val connection =
        DriverManager.getConnection("jdbc:sqlite:${AppConfig.storage.data}/${userId}/cloud/cloud.db")

    fun close() {
        connection.close()
    }

    init {
        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                        CREATE TABLE IF NOT EXISTS files (
                            id TEXT,
                            name TEXT,
                            parent_id TEXT,
                            type TEXT,
                            created INTEGER,
                            modified INTEGER,
                            uploaded INTEGER,
                            deleted INTEGER,
                            sha256 TEXT,
                            size INTEGER,
                            version INTEGER,
                            PRIMARY KEY (id, version)
                        );
                        """
                )
                statement.executeUpdate(
                    """
                        CREATE TABLE IF NOT EXISTS trash (
                            id TEXT,
                            name TEXT,
                            parent_id TEXT,
                            type TEXT,
                            created INTEGER,
                            modified INTEGER,
                            uploaded INTEGER,
                            deleted INTEGER,
                            permanently_deleted INTEGER,
                            sha256 TEXT,
                            size INTEGER,
                            version INTEGER
                        );
                        """
                )
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun generateUniqueFileId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        while (true) {
            val length = Random.nextInt(5) + 30
            val id = (1..length)
                .map { chars.random() }
                .joinToString("")

            val sql = "SELECT COUNT(*) FROM files WHERE id = ?"
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                val rs = stmt.executeQuery()
                if (rs.next() && rs.getInt(1) == 0) {
                    return id
                }
            }
        }
    }

    fun insertFile(fileModel: FileModel) {
        val sql =
            if (fileModel.deleted == null) {
                "INSERT INTO files (id, parent_id, name, type, created, modified, uploaded, sha256, size, version) VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            } else {
                "INSERT INTO files (id, parent_id, name, type, created, modified, uploaded, sha256, size, version, deleted) VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            }
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, fileModel.id)
        preparedStatement.setString(2, fileModel.parentId)
        preparedStatement.setString(3, fileModel.name)
        preparedStatement.setString(4, fileModel.type)
        preparedStatement.setLong(5, fileModel.created)
        preparedStatement.setLong(6, fileModel.modified)
        preparedStatement.setLong(7, fileModel.uploaded)
        preparedStatement.setString(8, fileModel.sha256)
        preparedStatement.setLong(9, fileModel.size)
        preparedStatement.setInt(10, fileModel.version)
        if(fileModel.deleted != null) {
            preparedStatement.setLong(11, fileModel.deleted)
        }
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun getFiles(location: String?): List<FileModel> {
        val list = mutableListOf<FileModel>()
        val sql =
            if (location == null) {
                """
            SELECT f.*
            FROM files f
            JOIN (
                SELECT id, MAX(version) AS max_version
                FROM files
                GROUP BY id
            ) latest ON f.id = latest.id AND f.version = latest.max_version;
            """
            } else {
                """
            SELECT f.*
            FROM files f
            JOIN (
                SELECT id, MAX(version) AS max_version
                FROM files
                GROUP BY id
            ) latest ON f.id = latest.id AND f.version = latest.max_version
            WHERE f.parent_id = ?;
            """
            }


        val statement = connection.prepareStatement(sql)
        if (location != null) {
            statement.setString(1, location)
        }
        val resultSet: ResultSet = statement.executeQuery()
        while (resultSet.next()) {
            list.add(FileModel.fromResultSet(resultSet))
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun getLatestFileModelById(id: String): FileModel? {
        var fileModel: FileModel? = null
        val sql = "SELECT * FROM files WHERE id = ? ORDER BY version DESC LIMIT 1;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet: ResultSet = statement.executeQuery()
        while (resultSet.next()) {
            fileModel = FileModel.fromResultSet(resultSet)
        }
        statement.close()
        return fileModel
    }

    fun deleteFile(fileModel: FileModel) {

        val list = getFileModelsById(fileModel.id)

        moveFilesToTrash(list)

        if(fileModel.type == "folder") {
            val sql = "SELECT * FROM files WHERE parent_id = ?;"
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, fileModel.id)
                val resultSet = statement.executeQuery()
                while(resultSet.next()) {
                    val child = FileModel.fromResultSet(resultSet)
                    deleteFile(child)
                }
            }
        }

        val deleteSql = "DELETE FROM files WHERE id = ?;"
        connection.prepareStatement(deleteSql).use { statement ->
            statement.setString(1, fileModel.id)
            statement.executeUpdate()
        }
    }

    fun getFileModelsById(id: String): List<FileModel> {
        val list = mutableListOf<FileModel>()
        val sql = "SELECT * FROM files WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet: ResultSet = statement.executeQuery()
        while (resultSet.next()) {
            val fileModel = FileModel.fromResultSet(resultSet)
            list.add(fileModel)
        }
        statement.close()
        return list
    }

    private fun moveFilesToTrash(list: List<FileModel>) {
        val sql = """
    INSERT INTO trash (id, name, parent_id, type, created, modified, uploaded, deleted, sha256, size, version, permanently_deleted)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""

        val statement = connection.prepareStatement(sql)

        for (file in list) {
            statement.setString(1, file.id)
            statement.setString(2, file.name)
            statement.setString(3, file.parentId)
            statement.setString(4, file.type)
            statement.setLong(5, file.created)
            statement.setLong(6, file.modified)
            statement.setLong(7, file.uploaded)
            statement.setObject(8, file.deleted)
            statement.setString(9, file.sha256)
            statement.setLong(10, file.size)
            statement.setInt(11, file.version)
            statement.setLong(12, Instant.now().toEpochMilli())
            statement.addBatch()
        }

        statement.executeBatch()
        statement.close()
    }

    fun permanentlyDeleteObsoleteFiles() {
        val cutoff = Instant.now().epochSecond - 30 * 24 * 60 * 60
        val sql = """
                    DELETE FROM trash
                    WHERE permanently_deleted <= ? OR permanently_deleted IS NULL
                  """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, cutoff)
        }
    }
}