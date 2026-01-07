package com.amphi.server.models.photos

import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Photo(
    var id: String,
    var title: String,
    var created: Long,
    var modified: Long,
    var date: Long,
    var deleted: Long? = null,
    var mimeType: String,
    var sha256: String,
    var note: String? = null,
    var tags: JsonArray? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : Photo {
            return Photo(
                id = resultSet.getString("id"),
                title = resultSet.getString("title"),
                created = resultSet.getLong("created"),
                modified = resultSet.getLong("modified"),
                date = resultSet.getLong("date"),
                deleted = resultSet.getNullableLong("deleted"),
                mimeType = resultSet.getString("mime_type"),
                sha256 = resultSet.getString("sha256"),
                note = resultSet.getNullableString("note"),
                tags = resultSet.getNullableJsonArray("tags")
            )
        }

        fun legacy(id: String, jsonObject: JsonObject) : Photo {
            return Photo(
                id = id,
                title = jsonObject.getString("title"),
                created = jsonObject.getLong("created"),
                modified = jsonObject.getLong("modified"),
                date = jsonObject.getLong("date"),
                deleted = jsonObject.getValue("deleted") as? Long,
                mimeType = jsonObject.getString("mimeType"),
                sha256 = jsonObject.getString("sha256"),
                note = jsonObject.getValue("note") as? String
            )
        }

        fun legacy(infoFile: File) : Photo {
            try {
                val jsonObject = JsonObject(infoFile.readText())
                return legacy(id = infoFile.parentFile.nameWithoutExtension, jsonObject = jsonObject)
            }
            catch (e: Exception) {
                println(e)
                return Photo(
                    id = infoFile.parentFile.nameWithoutExtension,
                    title = "",
                    created = 0,
                    modified = 0,
                    date = 0,
                    mimeType = "unknown",
                    sha256 = "unknown",
                    note = infoFile.readText()
                )
            }
        }
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("title", title)

        //TODO: remove added, mimeType in next version
        jsonObject.put("added", created)
        jsonObject.put("created", created)
        jsonObject.put("modified", modified)
        jsonObject.put("date", date)
        jsonObject.put("deleted", deleted)
        jsonObject.put("mimeType", mimeType)
        jsonObject.put("mime_type", mimeType)
        jsonObject.put("sha256", sha256)
        jsonObject.put("note", note)
        jsonObject.put("tags", tags)

        return jsonObject
    }
}