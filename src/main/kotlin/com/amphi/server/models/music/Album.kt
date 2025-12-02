package com.amphi.server.models.music

import com.amphi.server.utils.getJsonObject
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Album(
    var id: String,
    var title: JsonObject,
    var covers: JsonArray? = null,
    var genres: JsonArray? = null,
    var artistIds: JsonArray? = null,
    var added: Long,
    var modified: Long,
    var deleted: Long? = null,
    var released: Long? = null,
    var description: String? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet): Album {
            return Album(
                id = resultSet.getString("id"),
                title = resultSet.getJsonObject("title"),
                covers = resultSet.getNullableJsonArray("covers"),
                genres = resultSet.getNullableJsonArray("genres"),
                artistIds = resultSet.getNullableJsonArray("artist_ids"),
                added = resultSet.getLong("added"),
                modified = resultSet.getLong("modified"),
                deleted = resultSet.getLong("deleted"),
                released = resultSet.getNullableLong("released"),
                description = resultSet.getNullableString("description")
            )
        }

        fun legacy(id: String, jsonObject: JsonObject, covers: JsonArray): Album {
            val artistIds = JsonArray()
            (jsonObject.getValue("artist") as? String).let { artistId ->
                if(!artistId.isNullOrBlank()) {
                    artistIds.add(artistId)
                }
            }
            return Album(
                id = id,
                title = jsonObject.getJsonObject("title"),
                covers = covers,
                genres = JsonArray().add(jsonObject.getJsonObject("genre")),
                artistIds = artistIds,
                added = jsonObject.getLong("added"),
                modified = jsonObject.getLong("modified")
            )
        }

        fun legacy(infoFile: File) : Album {
            try {
                val jsonObject = JsonObject(infoFile.readText())
                val covers = JsonArray()
                infoFile.parentFile.listFiles()?.forEach { file ->
                    if(file.nameWithoutExtension != "info" && file.isFile) {
                        val fileData = JsonObject()
                        val imageId = file.nameWithoutExtension
                        fileData.put("id", imageId)
                        fileData.put("filename", file.name)
                        covers.add(fileData)
                    }
                }
                return legacy(
                    jsonObject = jsonObject,
                    id = infoFile.parentFile.nameWithoutExtension,
                    covers = covers
                )
            }
            catch (e: Exception) {
                println(e)
                return Album(
                    id = infoFile.parentFile.nameWithoutExtension,
                    title = JsonObject(),
                    added = 0,
                    modified = 0,
                    description = infoFile.readText()
                )
            }
        }
    }
}