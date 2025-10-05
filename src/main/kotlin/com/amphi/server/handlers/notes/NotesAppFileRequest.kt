package com.amphi.server.handlers.notes

import com.amphi.server.ServerDatabase
import com.amphi.server.sendAuthFailed
import com.amphi.server.sendFileNotExists
import com.amphi.server.sendSuccess
import com.amphi.server.sendUploadFailed
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object NotesAppFileRequest {

    fun getFiles(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val noteFileNameOnly = split[2]
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
            )
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val noteFileNameOnly = split[2]
        val filename = split[4]
        if(requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        }
        else {
            req.setExpectMultipart(true)
            req.uploadHandler { upload ->

                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                        sendAuthFailed(req)
                        println(requestToken)
                    },
                    onAuthenticated = { token ->
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
                )

            }
            req.exceptionHandler {
                sendUploadFailed(req)
            }
        }
    }

    fun downloadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val noteFileNameOnly = split[2]
        val filename = split[4]
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
                    val filePath = "users/${token.userId}/notes/notes/${noteFileNameOnly}/${directoryName}/${filename}"
                    val file = File(filePath)
                    if (!file.exists()) {
                        sendFileNotExists(req)
                    } else {
                        val contentType = when(file.extension) {
                            "mp3" -> "audio/mpeg"
                            "aac", "m4a" -> "audio/aac"
                            "flac" -> "audio/flac"
                            "wav" -> "audio/wav"
                            "ogg" -> "audio/ogg"
                            "opus" -> "audio/opus"
                            "amr" -> "audio/amr"
                            "weba" -> "audio/webm"
                            "mp4" -> "video/mp4"
                            "mkv" -> "video/x-matroska"
                            "webm" -> "video/webm"
                            "avi" -> "video/x-msvideo"
                            "mov" -> "video/quicktime"
                            "wmv" -> "video/x-ms-wmv"
                            "flv" -> "video/x-flv"
                            "3gp" -> "video/3gpp"
                            "3g2" -> "video/3gpp2"
                            else -> "application/octet-stream"
                        }
                        req.response().putHeader("content-type", contentType).sendFile(filePath)
                    }
                }
            )
        }

    }

    fun deleteFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val noteFileNameOnly = split[2]
        val filename = split[4]
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
                        ServerDatabase.notifyFileDelete("${trashes.path}/${filename}")
                        sendSuccess(req)
                    } else {
                        sendFileNotExists(req)
                    }

                }
            )
        }
    }
}