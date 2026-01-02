package com.amphi.server.models.music

import com.amphi.server.utils.getJsonArray
import com.amphi.server.utils.getJsonObject
import com.amphi.server.utils.getNullableInt
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Song(
    var id: String,
    var title: JsonObject,
    var genres: JsonArray? = null,
    var artistIds: JsonArray? = null,
    var albumId: String? = null,
    var created: Long,
    var modified: Long,
    var deleted: Long? = null,
    var composerIds: JsonArray? = null,
    var lyricistIds: JsonArray? = null,
    var arrangerIds: JsonArray? = null,
    var producerIds: JsonArray? = null,
    var archived: Boolean = false,
    var released: Long? = null,
    var trackNumber: Int? = null,
    var discNumber: Int? = null,
    var description: String? = null,
    var files: JsonArray,
    var featuredArtistIds: JsonArray? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : Song {
            return Song(
                id = resultSet.getString("id"),
                title = resultSet.getJsonObject("title"),
                genres = resultSet.getJsonArray("genres"),
                artistIds = resultSet.getNullableJsonArray("artist_ids"),
                albumId =  resultSet.getNullableString("album_id"),
                created = resultSet.getLong("created"),
                modified = resultSet.getLong("modified"),
                deleted = resultSet.getNullableLong("deleted"),
                composerIds = resultSet.getNullableJsonArray("composer_ids"),
                lyricistIds = resultSet.getNullableJsonArray("lyricist_ids"),
                arrangerIds = resultSet.getNullableJsonArray("arranger_ids"),
                producerIds = resultSet.getNullableJsonArray("producer_ids"),
                archived = resultSet.getBoolean("archived"),
                released = resultSet.getNullableLong("released"),
                trackNumber = resultSet.getNullableInt("track_number"),
                discNumber = resultSet.getNullableInt("disc_number"),
                description = resultSet.getNullableString("description"),
                files = resultSet.getJsonArray("files"),
            )
        }

        private fun legacyIdsValue(string: String?) : JsonArray? {
            if(!string.isNullOrBlank()) {
                val jsonArray = JsonArray()
                jsonArray.add(string)
                return jsonArray
            }
            return null
        }

        fun legacy(jsonObject: JsonObject, id: String, files: JsonArray) : Song {
            return Song(
                id = id,
                title = jsonObject.getJsonObject("title"),
                genres = jsonObject.getJsonArray("genre"),
                artistIds = legacyIdsValue(jsonObject.getValue("artist") as? String),
                albumId = jsonObject.getValue("album") as? String,
                created = jsonObject.getLong("added"),
                modified = jsonObject.getLong("modified"),
                deleted = null,
                composerIds = legacyIdsValue(jsonObject.getValue("composer") as? String),
                lyricistIds = legacyIdsValue(jsonObject.getValue("lyricist") as? String),
                arrangerIds = legacyIdsValue(jsonObject.getValue("arranger") as? String),
                producerIds = legacyIdsValue(jsonObject.getValue("producer") as? String),
                archived = jsonObject.getValue("archived") == true,
                released = jsonObject.getValue("released") as? Long,
                trackNumber = jsonObject.getValue("trackNumber") as? Int,
                discNumber = jsonObject.getValue("discNumber") as? Int,
                files = files
            )
        }

        private fun fileFormat(jsonObject: JsonObject, file: File) : String {
            val format = jsonObject.getValue("format")
            if(format is String && !format.isBlank() && format != "null") {
                return format
            }
            file.parentFile.listFiles()?.forEach { child ->
                if(child.nameWithoutExtension == file.nameWithoutExtension && child.isFile && child.extension != "json") {
                    return child.extension
                }
            }
            return "mp3"
        }

        fun legacy(infoFile: File) : Song {
            try {
                val jsonObject = JsonObject(infoFile.readText())
                val files = JsonArray()

                infoFile.parentFile.listFiles()?.forEach { file ->
                    if(file.nameWithoutExtension != "info" && file.isFile) {
                        if(file.extension == "json") {
                            runCatching {
                                val fileData = JsonObject()
                                val songFileId = file.nameWithoutExtension
                                val legacyData = JsonObject(file.readText())
                                fileData.put("id", songFileId)
                                fileData.put("filename", "${file.nameWithoutExtension}.${fileFormat(legacyData, file)}")
                                fileData.put("format", fileFormat(legacyData, file))
                                fileData.put("lyrics", legacyData.getValue("lyrics"))
                                fileData.put("priority", 0)
                                files.add(fileData)
                            }.getOrElse {
                                val fileData = JsonObject()
                                val songFileId = file.nameWithoutExtension
                                fileData.put("id", songFileId)
                                fileData.put("filename", "${file.nameWithoutExtension}.${fileFormat(JsonObject(), file)}")
                                fileData.put("format", fileFormat(JsonObject(), file))
                                fileData.put("priority", 0)
                                files.add(fileData)
                            }
                        }
                        else {
                            val songFileInfoFile = File(file.parentFile, "${file.nameWithoutExtension}.json")
                            if(!songFileInfoFile.exists()) {
                                val fileData = JsonObject()
                                val songFileId = file.nameWithoutExtension
                                fileData.put("id", songFileId)
                                fileData.put("filename", file.name)
                                fileData.put("format", file.extension)
                                fileData.put("priority", 0)
                                files.add(fileData)
                            }
                        }
                    }
                }
                return legacy(jsonObject = jsonObject, id = infoFile.parentFile.nameWithoutExtension, files = files)
            }
            catch (e: Exception) {
                println(e)
                return Song(
                    id = infoFile.parentFile.nameWithoutExtension,
                    title = JsonObject(),
                    genres = JsonArray(),
                    created = 0,
                    modified = 0,
                    description = infoFile.readText(),
                    files = JsonArray(),
                )
            }
        }
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("title", title)
        jsonObject.put("genres", genres)

        jsonObject.put("artist_ids", artistIds)
        jsonObject.put("album_id", albumId)

        //TODO: remove added in next version
        jsonObject.put("added", created)
        jsonObject.put("created", created)
        jsonObject.put("modified", modified)
        jsonObject.put("deleted", deleted)

        jsonObject.put("composer_ids", composerIds)
        jsonObject.put("lyricist_ids", lyricistIds)
        jsonObject.put("arranger_ids", arrangerIds)
        jsonObject.put("producer_ids", producerIds)

        jsonObject.put("archived", archived)

        jsonObject.put("released", released)
        jsonObject.put("track_number", trackNumber)
        jsonObject.put("disc_number", discNumber)

        jsonObject.put("description", description)

        jsonObject.put("files", files)

        jsonObject.put("featured_artist_ids", featuredArtistIds)

        return jsonObject
    }
}