package com.amphi.server.models

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File

class Note(
    var id: String,
    var title: String? = null,
    var subtitle: String? = null,
    var backgroundColor: Long? = null,
    var background: String? = null,
    var textColor: Long? = null,
    var textSize: Double? = null,
    var lineHeight: Double? = null,
    var parentId: String = "",
    var modified: Long,
    val created: Long,
    var deleted: Long? = null,
    val content: JsonArray,
    val isFolder: Boolean = false,
    var version: Int? = null
) {

    companion object {
        fun legacy(file: File) : Note {
            try {
                val jsonObject = JsonObject(file.readText())
                println(jsonObject)

                if(file.extension == "folder") {
                    return Note(
                        id = file.nameWithoutExtension,
                        title = jsonObject.getString("name"),
                        content = JsonArray(),
                        created = jsonObject.getLong("created"),
                        modified = jsonObject.getLong("modified"),
                        deleted = jsonObject.getValue("deleted") as? Long,
                        isFolder = true,
                        parentId = jsonObject.getString("location").split(".folder").first()
                    )
                }
                else {
                    return Note(
                        id = file.nameWithoutExtension,
                        content = jsonObject.getJsonArray("contents"),
                        created = jsonObject.getLong("created"),
                        modified = jsonObject.getLong("modified"),
                        deleted = jsonObject.getValue("deleted") as? Long,
                        isFolder = false,
                        backgroundColor = jsonObject.getValue("backgroundColor") as? Long,
                        textColor = jsonObject.getValue("textColor") as? Long,
                        textSize = jsonObject.getValue("textSize") as? Double,
                        lineHeight = jsonObject.getValue("lineHeight") as? Double,
                        parentId = jsonObject.getString("location").split(".folder").first()
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

}