package com.amphi.handlers

import com.amphi.*
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object FileRequestHandler {

    fun handleSimpleFileUpload(req: HttpServerRequest, path: String, appType: String, action: String) {
        req.setExpectMultipart(true)

        req.uploadHandler { upload ->
            val requestToken = req.headers()["Authorization"]

            if (requestToken.isNullOrBlank()) {
                 sendAuthFailed(req)
            } else {
                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                         sendAuthFailed(req)
                        println(requestToken)
                    },
                    onAuthenticated = { token ->
                        val directory = File("users/${token.userId}")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }

                        upload.streamToFileSystem("users/${token.userId}/$path").onComplete { ar ->
                            if (ar.succeeded()) {
                                req.response().end(Messages.SUCCESS)
                            } else {
                                req.response().setStatusCode(500).end(Messages.UPLOAD_FAILED)
                            }
                        }
                        ServerDatabase.saveEvent(
                            token = token,
                            action = action,
                            value = upload.filename(),
                            appType = appType
                        )
                    }
                )


            }

        }
        req.exceptionHandler {
            req.response().setStatusCode(StatusCode.INTERNAL_SERVER_ERROR).end(Messages.FAILED)
        }
    }

    fun handleFileUpload(req: HttpServerRequest, filePath: String, appType: String, action: String) {
        req.setExpectMultipart(true)
        req.uploadHandler { upload ->
            val requestToken = req.headers()["Authorization"]

            if (requestToken.isNullOrBlank() || upload.filename().isNullOrBlank() || upload.filename().trim()
                    .isBlank()
            ) {
                 sendAuthFailed(req)
            } else {
                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                         sendAuthFailed(req)
                        println(requestToken)
                    },
                    onAuthenticated = { token ->
                        val file = File("users/${token.userId}/$filePath")
                        if (!file.exists()) {
                            file.mkdirs()
                        }

                        upload.streamToFileSystem("users/${token.userId}/$filePath/${upload.filename()}").onComplete { ar ->
                            if (ar.succeeded()) {
                                ServerDatabase.saveEvent(
                                    token = token,
                                    action = action,
                                    value = upload.filename(),
                                    appType = appType
                                )
                                println("upload ${LocalDateTime.now()}")
                                req.response().end(Messages.SUCCESS)
                            } else {
                                req.response().setStatusCode(StatusCode.INTERNAL_SERVER_ERROR).end(Messages.UPLOAD_FAILED)
                            }
                        }

                    }
                )


            }

        }
        req.exceptionHandler {
            req.response().setStatusCode(StatusCode.INTERNAL_SERVER_ERROR).end(Messages.FAILED)
        }
    }

    fun handleSimpleFileDownload(req: HttpServerRequest, path: String) {
        val requestToken = req.headers()["Authorization"]
        if (requestToken.isNullOrBlank()) {
             sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                     sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val filePath = "users/${token.userId}/$path"
                    if (!File(filePath).exists()) {
                        req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
                    } else {
                        req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
                    }

                }
            )

        }
    }

    fun handleFileDownload(req: HttpServerRequest, path: String) {
        val requestToken = req.headers()["Authorization"]
        if (requestToken.isNullOrBlank()) {
             sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                     sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val filePath = "users/${token.userId}/$path"
                    println("users/${token.userId}/$path")
                    if (!File(filePath).exists()) {
                        req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
                    } else {
                        req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
                    }
                }
            )
        }
    }

    fun handleGetFile(req: HttpServerRequest, path: String) {
        val requestToken = req.headers()["Authorization"]
        if (requestToken.isNullOrBlank()) {
             sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                     sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val jsonArray = JsonArray()
                    val directory = File("users/${token.userId}/$path")
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

    /***/
    fun handleDeleteFile(req: HttpServerRequest, path: String, appType: String, action: String) {
        val requestToken = req.headers()["Authorization"]
        val filename = req.params()["filename"]
        if (requestToken.isNullOrBlank() || filename.isNullOrBlank()) {
             sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                     sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val file = File("users/${token.userId}/$path/$filename")
                    val trashes = File("users/${token.userId}/trashes/$path")
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
                            action = action,
                            value = filename,
                            appType = appType
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