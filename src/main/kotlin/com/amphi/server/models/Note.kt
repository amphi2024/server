package com.amphi.server.models

import io.vertx.core.json.JsonObject
import java.io.File

class Note(val file: File) {
    val filename = file.name
    val name = file.nameWithoutExtension
    val contents = mutableListOf<NoteContent>()

    init {
        try {
            val jsonObject = JsonObject(file.readText())
            val contentsData = jsonObject.getJsonArray("contents")
            contentsData.forEach { data ->
                if(data is JsonObject) {
                    val styleData = data.getValue("style")
                    val style = mutableMapOf<String, Any>()
                    if(styleData is JsonObject) {
                        styleData.fieldNames().forEach { key ->
                            style[key] = styleData.getValue(key)
                        }
                    }
                    val content = NoteContent(
                        value = data.getValue("value"),
                        type = data.getString("type"),
                        style = style
                    )
                    contents.add(content)
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

    }

}