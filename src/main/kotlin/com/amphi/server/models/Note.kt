package com.amphi.server.models

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.sql.ResultSet

class Note(
    var id: String,
    var title: String? = null,
    var subtitle: String? = null,
    var backgroundColor: Long? = null,
    var background: String? = null,
    var textColor: Long? = null,
    var textSize: Int? = null,
    var lineHeight: Int? = null,
    var parentId: String? = null,
    var modified: Long,
    val created: Long,
    var deleted: Long? = null,
    val content: JsonArray,
    val isFolder: Boolean = false
) {

    companion object {

        fun fromResultSet(resultSet: ResultSet) : Note {
            return Note(
                id = resultSet.getString("id"),
                title = resultSet.getObject("title") as? String,
                subtitle = resultSet.getObject("subtitle") as? String,
                backgroundColor = resultSet.getObject("background_color")?.let { (it as Number).toLong() },
                background = resultSet.getObject("background") as? String,
                textColor = resultSet.getObject("text_color")?.let { (it as Number).toLong() },
                textSize = resultSet.getObject("text_size")?.let { (it as Number).toInt() },
                lineHeight = resultSet.getObject("line_height")?.let { (it as Number).toInt() },
                parentId = resultSet.getString("parent_id") ?: "",
                modified = resultSet.getLong("modified"),
                created = resultSet.getLong("created"),
                deleted = resultSet.getObject("deleted")?.let { (it as Number).toLong() },
                content = resultSet.getObject("content")?.let { value ->
                    JsonArray(value as? String)
                } ?: JsonArray(),
                isFolder = resultSet.getBoolean("is_folder")
            )
        }

        fun legacy(file: File) : Note {
            try {
                val jsonObject = JsonObject(file.readText())
                val location = jsonObject.getValue("location") as? String
                var parentId = location?.split(".folder")?.firstOrNull()
                if(parentId != null) {
                    parentId = if(parentId.isEmpty()) {
                        null
                    } else {
                        "${parentId}legacyfolder"
                    }
                }

                if(file.extension == "folder") {
                    return Note(
                        id = "${file.nameWithoutExtension}legacyfolder",
                        title = jsonObject.getString("name"),
                        content = JsonArray(),
                        created = jsonObject.getLong("created"),
                        modified = jsonObject.getLong("modified"),
                        deleted = jsonObject.getValue("deleted") as? Long,
                        isFolder = true,
                        parentId = parentId
                    )
                }
                else {
                    return Note(
                        id = "${file.nameWithoutExtension}legacynote",
                        content = jsonObject.getJsonArray("contents"),
                        created = jsonObject.getLong("created"),
                        modified = jsonObject.getLong("modified"),
                        deleted = jsonObject.getValue("deleted") as? Long,
                        isFolder = false,
                        backgroundColor = jsonObject.getValue("backgroundColor") as? Long,
                        textColor = jsonObject.getValue("textColor") as? Long,
                        textSize = jsonObject.getValue("textSize") as? Int,
                        lineHeight = jsonObject.getValue("lineHeight") as? Int,
                        parentId = parentId
                    )
                }
            }
            catch (e: Exception) {
                println(e)
                return Note(
                    id = file.nameWithoutExtension,
                    content = JsonArray().add(file.readText()),
                    created = 0,
                    modified = 0
                )
            }
        }
    }

    fun deleteObsoleteMediaFiles(files: MutableList<File>?, mediaContents: MutableList<NoteContent>) {
        if (files != null) {
            val imageIterator = files.listIterator()
            while (imageIterator.hasNext()) {
                val imageFile = imageIterator.next()
                val mediaIterator = mediaContents.listIterator()
                while (mediaIterator.hasNext()) {
                    if (mediaIterator.next().value == imageFile.name) {
                        mediaIterator.remove()
                        imageIterator.remove()
                        break
                    }
                }
            }
            files.forEach { file ->
                file.delete()
            }
        }
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("title", title)
        jsonObject.put("subtitle", subtitle)
        jsonObject.put("background_color", backgroundColor)
        jsonObject.put("background", background)
        jsonObject.put("text_color", textColor)
        jsonObject.put("textSize", textSize)
        jsonObject.put("line_height", lineHeight)
        jsonObject.put("parent_id", parentId)
        jsonObject.put("modified", modified)
        jsonObject.put("created", created)
        jsonObject.put("deleted", deleted)
        jsonObject.put("content", if(isFolder) null else content.toString())
        jsonObject.put("is_folder", if(isFolder) 1 else 0)

        return jsonObject
    }

}