package com.amphi.server.models.music

import com.amphi.server.utils.getJsonArray
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Playlist(
    var id: String,
    var title: String,
    var songs: JsonArray,
    var created: Long,
    var modified: Long,
    var deleted: Long? = null,
    var thumbnails: JsonArray? = null,
    var note: String? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : Playlist {
            return Playlist(
                id = resultSet.getString("id"),
                title = resultSet.getString("title"),
                songs = resultSet.getJsonArray("songs"),
                created = resultSet.getLong("created"),
                modified = resultSet.getLong("modified"),
                deleted = resultSet.getNullableLong("deleted"),
                thumbnails = resultSet.getNullableJsonArray("thumbnails"),
                note = resultSet.getNullableString("note")
            )
        }

        fun legacy(id: String, jsonObject: JsonObject, created: Long, modified: Long) : Playlist {
            return Playlist(
                id = id,
                title = jsonObject.getString("title"),
                songs = jsonObject.getJsonArray("songs"),
                created = created,
                modified = modified
            )
        }

        fun legacy(file: File) : Playlist {
            try {
                val jsonObject = JsonObject(file.readText())
                return legacy(
                    jsonObject = jsonObject,
                    id = file.nameWithoutExtension,
                    created = file.lastModified(),
                    modified = file.lastModified()
                )
            }
            catch (e: Exception) {
                println(e)
                return Playlist(
                    id = file.nameWithoutExtension,
                    title = "",
                    songs = JsonArray(),
                    created = 0,
                    modified = 0,
                    note = file.readText()
                )
            }
        }
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("title", title)
        jsonObject.put("songs", songs)
        jsonObject.put("created", created)
        jsonObject.put("modified", modified)
        jsonObject.put("deleted", deleted)
        jsonObject.put("thumbnails", thumbnails)
        jsonObject.put("note", note)

        return jsonObject
    }
}