package com.amphi.server.handlers

import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendNotFound
import com.amphi.server.eventService
import com.amphi.server.models.notes.Note
import com.amphi.server.models.notes.NotesDatabase
import com.amphi.server.models.notes.NotesTheme
import com.amphi.server.models.notes.NotesThemeColors
import com.amphi.server.trashService
import com.amphi.server.utils.moveToTrash
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object NotesHandler {

    fun getNotes(req: HttpServerRequest) {
        handleAuthorization(req) { token ->
            val database = NotesDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getNotes().forEach { note ->
                val jsonObject = JsonObject()
                jsonObject.put("id", note.id)
                jsonObject.put("created", note.created)
                jsonObject.put("modified", note.modified)
                jsonArray.add(jsonObject)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())

            database.close()
        }
    }

    fun uploadNote(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = NotesDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(id.endsWith(".note") || id.endsWith(".folder")) {
                    print(id.split(".").last())
                    val note = Note.legacy(
                        oldId = id.split(".").first(),
                        fileExtension = id.split(".").last(),
                        jsonObject = jsonObject
                    )
                    database.insertNote(note)
                    eventService.saveEvent(token = token, action = "upload_note", value = id, appType = "notes")

                    sendSuccess(req)
                    database.close()
                    return@bodyHandler
                }
                val note = Note(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getValue("title") as? String,
                    subtitle = jsonObject.getValue("subtitle") as? String,
                    backgroundColor = jsonObject.getValue("background_color") as? Long,
                    background = jsonObject.getValue("background") as? String,
                    textColor = jsonObject.getValue("text_color") as? Long,
                    textSize = jsonObject.getValue("text_size") as? Int,
                    lineHeight = jsonObject.getValue("line_height") as? Int,
                    parentId = jsonObject.getValue("parent_id") as? String,
                    modified = jsonObject.getLong("modified"),
                    created = jsonObject.getLong("created"),
                    deleted = jsonObject.getValue("deleted") as? Long,
                    content = jsonObject.getJsonArray("content"),
                    isFolder = jsonObject.getBoolean("is_folder")
                )
                database.insertNote(note)
                eventService.saveEvent(token = token, action = "upload_note", value = id, appType = "notes")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun downloadNote(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val database = NotesDatabase(token.userId)
            val note = database.getNoteById(id)
            if (note == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(note.toJsonObject().toString())
            }
            database.close()
        }
    }

    fun deleteNote(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val database = NotesDatabase(token.userId)
            database.setNoteDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_note",
                value = id,
                appType = "notes"
            )
            sendSuccess(req)
            database.close()
        }
    }

    fun getFiles(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/notes/attachments/${id[0]}/${id[1]}/$id/${directoryName}")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    val jsonObject = JsonObject()
                    jsonObject.put("filename", file.name)
                    jsonObject.put("modified", file.lastModified())
                    jsonObject.put("size", file.length())
                    jsonArray.add(jsonObject)
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[2]
        val filename = split[4]

        handleAuthorization(req) {token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val directory = File("users/${token.userId}/notes/attachments/${id[0]}/${id[1]}/$id/${directoryName}")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                upload.streamToFileSystem("${directory.path}/${filename}").onComplete { ar ->
                    if (ar.succeeded()) {
                        sendSuccess(req)
                    } else {
                        sendUploadFailed(req)
                    }
                }
            }
            req.exceptionHandler {
                sendUploadFailed(req)
            }
        }
    }

    fun downloadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[2]
        val filename = split[4]
        handleAuthorization(req) { token ->
            val filePath = "users/${token.userId}/notes/attachments/${id[0]}/${id[1]}/${id}/${directoryName}/${filename}"
            val file = File(filePath)
            if (!file.exists()) {
                sendFileNotExists(req)
            } else {
                req.response().putHeader("content-type", contentTypeByExtension(file.extension)).sendFile(filePath)
            }
        }
    }

    fun deleteFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[2]
        val filename = split[4]
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/notes/attachments/${id[0]}/${id[1]}/${id}/${directoryName}/${filename}")
            if (file.exists()) {
                moveToTrash(
                    userId = token.userId,
                    path = "notes/attachments/${id[0]}/${id[1]}/${id}/${directoryName}",
                    filename = filename
                )
                sendSuccess(req)
            } else {
                sendFileNotExists(req)
            }
        }
    }

    fun getThemes(req: HttpServerRequest) {
        handleAuthorization(req) { token ->

            val database = NotesDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getThemes().forEach { theme ->
                jsonArray.add(theme.toJsonObject())
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())

            database.close()
        }
    }

    fun uploadTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = NotesDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(id.endsWith(".theme")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val theme = NotesTheme(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    modified = jsonObject.getLong("modified"),
                    created = jsonObject.getLong("created"),
                    lightColors = NotesThemeColors(
                        background = jsonObject.getLong("background_light"),
                        text = jsonObject.getLong("text_light"),
                        accent = jsonObject.getLong("accent_light"),
                        card = jsonObject.getLong("card_light"),
                        floatingButtonBackground = jsonObject.getLong("floating_button_background_light"),
                        floatingButtonIcon = jsonObject.getLong("floating_button_icon_light")
                    ),
                    darkColors = NotesThemeColors(
                        background = jsonObject.getLong("background_dark"),
                        text = jsonObject.getLong("text_dark"),
                        accent = jsonObject.getLong("accent_dark"),
                        card = jsonObject.getLong("card_dark"),
                        floatingButtonBackground = jsonObject.getLong("floating_button_background_dark"),
                        floatingButtonIcon = jsonObject.getLong("floating_button_icon_dark")
                    )
                )
                database.insertTheme(theme)
                eventService.saveEvent(token = token, action = "upload_theme", value = id, appType = "notes")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun downloadTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = NotesDatabase(token.userId)
            val theme = database.getThemeById(id)
            if (theme == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(theme.toJsonObject().toString())
            }
            database.close()
        }
    }

    fun deleteTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = NotesDatabase(token.userId)
            database.deleteTheme(id)
            eventService.saveEvent(
                token = token,
                action = "delete_theme",
                value = id,
                appType = "notes"
            )
            sendSuccess(req)
            database.close()
        }
    }
}