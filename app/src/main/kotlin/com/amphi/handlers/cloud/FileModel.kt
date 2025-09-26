package com.amphi.handlers.cloud

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.nio.charset.StandardCharsets
import java.sql.ResultSet

data class FileModel(
    val id: String,
    val parentId: String,
    val name: String,
    val type: String,
    val created: Long,
    val modified: Long,
    val uploaded: Long,
    val deleted: Long? = null,
    val sha256: String,
    val size: Long,
    var version: Int
) {
    companion object{

        fun fromRequestBuffer(id: String, buffer: Buffer) : FileModel {
            val info = JsonObject(buffer.toString(StandardCharsets.UTF_8))
            return  FileModel(
                id = id,
                name = info.getString("name") ?: "",
                created = info.getLong("created") ?: 0,
                type = info.getString("type") ?: "",
                modified = info.getLong("modified") ?: 0,
                parentId = info.getString("parent_id") ?: "",
                uploaded = info.getLong("uploaded") ?: 0,
                sha256 = info.getString("sha256") ?: "",
                size = info.getLong("size") ?: 0,
                version = 1,
                deleted = info.getValue("deleted") as? Long
            )
        }

        fun fromResultSet(resultSet: ResultSet): FileModel {
            return FileModel(
                id = resultSet.getString("id"),
                parentId = resultSet.getString("parent_id"),
                name = resultSet.getString("name"),
                type = resultSet.getString("type"),
                created = resultSet.getLong("created"),
                modified = resultSet.getLong("modified"),
                uploaded = resultSet.getLong("uploaded"),
                deleted = resultSet.getObject("deleted")?.let { (it as Number).toLong() },
                sha256 = resultSet.getString("sha256"),
                size = resultSet.getLong("size"),
                version = resultSet.getInt("version")
            )
        }
    }

    fun toJsonObject() : JsonObject {
        val jsonObject = JsonObject()
        jsonObject.put("id", id)
        jsonObject.put("parent_id", parentId)
        jsonObject.put("name", name)
        jsonObject.put("modified", modified)
        jsonObject.put("created", created)
        jsonObject.put("uploaded", uploaded)
        jsonObject.put("deleted", deleted)
        jsonObject.put("type", type)
        jsonObject.put("sha256", sha256)
        jsonObject.put("size", size)

        return jsonObject
    }
}