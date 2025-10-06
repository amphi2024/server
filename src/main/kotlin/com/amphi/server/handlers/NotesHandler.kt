package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.common.handleAuthorization
import com.amphi.server.eventService
import com.amphi.server.trashService
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
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/notes/notes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    val jsonObject = JsonObject()
                    jsonObject.put("filename", file.name)
                    jsonObject.put("modified", file.lastModified())
                    jsonArray.add(jsonObject)
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun uploadNote(req: HttpServerRequest, split: List<String>) {
        val filename = split[2]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val file = File("users/${token.userId}/notes/notes/${filename}")
                file.writeText(buffer.toString())
                eventService.saveEvent(token = token, action = "upload_note", value = filename, appType = "notes")

                sendSuccess(req)
            }
        }
    }

    fun downloadNote(req: HttpServerRequest, split: List<String>) {
        val filename = split[2]
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/notes/notes/${filename}")
            if (!file.exists()) {
                sendFileNotExists(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
            }
        }
    }


    fun deleteNote(req: HttpServerRequest, split: List<String>) {
        val filename = split[2]
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/notes/notes/$filename")
            val trashes = File("users/${token.userId}/trashes/notes/notes")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${filename}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                trashService.notifyFileDelete("${trashes.path}/${filename}")
                eventService.saveEvent(
                    token = token,
                    action = "delete_note",
                    value = filename,
                    appType = "notes"
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }
        }
    }

    fun getFiles(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val noteFileNameOnly = split[2]
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/notes/notes/$noteFileNameOnly/${directoryName}")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    val jsonObject = JsonObject()
                    jsonObject.put("filename", file.name)
                    jsonObject.put("modified", file.lastModified()
                    )
                    jsonArray.add(jsonObject)
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val noteFileNameOnly = split[2]
        val filename = split[4]

        handleAuthorization(req) {token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val directory = File("users/${token.userId}/notes/notes/$noteFileNameOnly/${directoryName}")
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
        val noteFileNameOnly = split[2]
        val filename = split[4]
        handleAuthorization(req) { token ->
            val filePath = "users/${token.userId}/notes/notes/${noteFileNameOnly}/${directoryName}/${filename}"
            val file = File(filePath)
            if (!file.exists()) {
                sendFileNotExists(req)
            } else {
                req.response().putHeader("content-type", contentTypeByExtension(file.extension)).sendFile(filePath)
            }
        }
    }

    fun deleteFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val noteFileNameOnly = split[2]
        val filename = split[4]
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/notes/notes/$noteFileNameOnly/${directoryName}/$filename")
            val trashes = File("users/${token.userId}/trashes/notes/notes/$noteFileNameOnly/${directoryName}")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${filename}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                trashService.notifyFileDelete("${trashes.path}/${filename}")
                sendSuccess(req)
            } else {
                sendFileNotExists(req)
            }
        }
    }
}