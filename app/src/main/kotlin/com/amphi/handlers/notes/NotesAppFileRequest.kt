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
                            )  // ex: 2024;7;13;18;30;13
                            jsonArray.add(jsonObject)
                        }
                    }
                    req.response().putHeader("content-type", "application/json").end(jsonArray.encode())
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
                    println("download ${directoryName}: ${filePath}")
                    if (!File(filePath).exists()) {
                        sendFileNotExists(req)
                    } else {
                        req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
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