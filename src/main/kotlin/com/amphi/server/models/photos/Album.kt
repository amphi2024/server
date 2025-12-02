package com.amphi.server.models.photos

import com.amphi.server.utils.getJsonArray
import com.amphi.server.utils.getNullableInt
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Album(
    var id: String,
    var title: String,
    var created: Long,
    var modified: Long,
    var deleted: Long? = null,
    var photos: JsonArray,
    var coverPhotoIndex: Int? = null,
    var note: String? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet): Album {
            return Album(
                id = resultSet.getString("id"),
                title = resultSet.getString("title"),
                created = resultSet.getLong("created"),
                modified = resultSet.getLong("modified"),
                deleted = resultSet.getNullableLong("deleted"),
                photos = resultSet.getJsonArray("photos"),
                coverPhotoIndex = resultSet.getNullableInt("cover_photo_index"),
                note = resultSet.getNullableString("note")
            )
        }

        fun legacy(id: String, jsonObject: JsonObject): Album {
            return Album(
                id = id,
                title = jsonObject.getString("title"),
                created = jsonObject.getLong("created"),
                modified = jsonObject.getLong("modified"),
                deleted = jsonObject.getValue("deleted") as? Long,
                photos = jsonObject.getJsonArray("photos")
            )
        }

        fun legacy(file: File): Album {
            try {
                val jsonObject = JsonObject(file.readText())
                return legacy(
                    id = file.nameWithoutExtension,
                    jsonObject = jsonObject
                )
            } catch (e: Exception) {
                println(e)
                return Album(
                    id = file.nameWithoutExtension,
                    title = "unknown",
                    created = 0,
                    modified = 0,
                    photos = JsonArray(),
                    note = file.readText()
                )
            }
        }
    }
}