package com.amphi.handlers.notes

import com.amphi.*
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId

object NotesAppNoteRequest {

    fun getNotes(req: HttpServerRequest) {
        val requestToken = req.headers()["Authorization"]
        if(requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        }
        else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
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
                            jsonObject.put(
                                "modified", Instant.ofEpochMilli(file.lastModified())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime().stringify()
                            )  // ex: 2024;7;13;18;30;13
                            jsonArray.add(jsonObject)
                        }
                    }
                    req.response().putHeader("content-type", "application/json").end(jsonArray.encode())
                }
            )
        }
    }

    fun uploadNote(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val filename = split[2]
        if(requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        }
        else {
            req.bodyHandler { buffer->

                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                        sendAuthFailed(req)
                    },
                    onAuthenticated = { token ->
                        val file = File("users/${token.userId}/notes/notes/${filename}")
                        file.writeText(buffer.toString())
                        ServerDatabase.saveEvent(token = token, action = "upload_note", value = filename, appType = "notes")

                        sendSuccess(req)
                    }
                )
            }
        }
    }

    fun downloadNote(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val filename = split[2]
        if(requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        }
        else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val filePath = "users/${token.userId}/notes/notes/${filename}"
                    println("download note: ${filePath}")
                    val file = File("users/${token.userId}/notes/notes/${filename}")
                    if (!file.exists()) {
                        sendFileNotExists(req)
                    } else {
                        req.response().putHeader("content-type", "application/json").end(file.readText())
                    }
                }
            )
        }
    }

    fun deleteNote(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val filename = split[2]
        if (requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
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
                        ServerDatabase.notifyFileDelete("${trashes.path}/${filename}")
                        ServerDatabase.saveEvent(
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
            )

        }
    }
}