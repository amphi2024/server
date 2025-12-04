package com.amphi.server.models.music

import com.amphi.server.utils.getJsonObject
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Artist(
    var id: String,
    var name: JsonObject,
    var images: JsonArray? = null,
    var members: JsonArray? = null,
    var added: Long,
    var modified: Long,
    var deleted: Long? = null,
    var debut: Long? = null,
    var country: String? = null,
    var description: String? = null
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : Artist {
            return Artist(
                id = resultSet.getString("id"),
                name = resultSet.getJsonObject("name"),
                images = resultSet.getNullableJsonArray("images"),
                members = resultSet.getNullableJsonArray("members"),
                added = resultSet.getLong("added"),
                modified = resultSet.getLong("modified"),
                deleted = resultSet.getNullableLong("deleted"),
                debut = resultSet.getNullableLong("debut"),
                country = resultSet.getNullableString("country"),
                description = resultSet.getNullableString("description")
            )
        }

        fun legacy(jsonObject: JsonObject, id: String, images: JsonArray) : Artist {
            return Artist(
                id = id,
                name = jsonObject.getJsonObject("name"),
                images = images,
                added = jsonObject.getLong("added"),
                modified = jsonObject.getLong("modified")
            )
        }

        fun legacy(infoFile: File) : Artist {
            try {
                val jsonObject = JsonObject(infoFile.readText())
                val images = JsonArray()
                infoFile.parentFile.listFiles()?.forEach { file ->
                    if(file.nameWithoutExtension != "info" && file.isFile) {
                        val fileData = JsonObject()
                        val imageId = file.nameWithoutExtension
                        fileData.put("id", imageId)
                        fileData.put("filename", file.name)
                        images.add(fileData)
                    }
                }
                return legacy(jsonObject = jsonObject, id = infoFile.parentFile.nameWithoutExtension, images = images)
            }
            catch (e: Exception) {
                println(e)
                return Artist(
                    id = infoFile.parentFile.nameWithoutExtension,
                    name = JsonObject(),
                    added = 0,
                    modified = 0,
                    description = infoFile.readText()
                )
            }
        }
    }


    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("images", images)
        jsonObject.put("members", members)
        jsonObject.put("added", added)
        jsonObject.put("modified", modified)
        jsonObject.put("deleted", deleted)
        jsonObject.put("debut", debut)
        jsonObject.put("country", country)
        jsonObject.put("description", description)

        return jsonObject
    }
}