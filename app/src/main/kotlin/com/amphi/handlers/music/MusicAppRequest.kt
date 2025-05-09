package com.amphi.handlers.music

import com.amphi.*
import com.amphi.models.Token
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

interface MusicAppRequest {

    fun item(token: Token, id: String, directoryName: String) : File {
        val directory = File("users/${token.userId}/music/${directoryName}/${id.substring(0, 1)}/${id.substring(1, 2)}/$id")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun getItems(req: HttpServerRequest, directoryName: String) {
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
                    val directory = File("users/${token.userId}/music/${directoryName}")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    directory.listFiles()?.forEach { subDir ->  //    /music/songs/a
                        subDir.listFiles()?.forEach { subDir2 -> //    /music/songs/a/b
                            subDir2.listFiles()?.forEach { file -> //    /music/songs/a/b/{abMusic}
                                jsonArray.add(file.name)
                            }
                        }
                    }
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
                }
            )
        }
    }

    fun getFilesOfSomething(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
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
                    val directory = item(token, id, directoryName)
                    val files = directory.listFiles()
                    if (files != null) {
                        for (file in files) {
                            if(file.name != "info.json") {
                                val jsonObject = JsonObject()
                                jsonObject.put("filename", file.name)
                                jsonObject.put("modified", file.lastModified())
                                jsonArray.add(jsonObject)
                            }
                        }
                    }
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
                }
            )
        }
    }

    fun getInfo(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
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
                    val directory = item(token, id, directoryName)
                    val infoFile = File("${directory.path}/info.json")
                    try {
                        req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
                    }
                    catch (e: Exception) {
                        sendNotFound(req)
                    }
                }
            )
        }
    }

    fun uploadInfo(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
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
                        val directory =  item(token, id, directoryName)

                        val file = File("${directory.path}/info.json")
                        file.writeText(buffer.toString())
                        ServerDatabase.saveEvent(token = token, action = eventName, value = id, appType = "music")

                        sendSuccess(req)
                    }
                )
            }
        }
    }

    // /music/songs/my-song/my-file.lyrics
    // /music/songs/my-song/my-file.mp3
    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
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
                    },
                    onAuthenticated = { token ->
                        val directory = item(token, id, directoryName)

                        upload.streamToFileSystem("${directory.path}/${filename}").onComplete { ar ->
                            if (ar.succeeded()) {
                                ServerDatabase.saveEvent(token = token, action = eventName, value = "$id;$filename", appType = "music")
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

    // /music/songs/my-song/my-file.lyrics
    // /music/albums/my-album/my-cover.jpg
    fun downloadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
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
                    val filePath = "users/${token.userId}/music/${directoryName}/${id.substring(0, 1)}/${id.substring(1, 2)}/${id}/${filename}"
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


    // /music/songs/my-song/my-file.lyrics
    // /music/songs/my-song/my-file.mp3
    fun delete(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val requestToken = req.headers()["Authorization"]
        val filename = split[3]
        if (requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val file = File("users/${token.userId}/music/${directoryName}/${filename.substring(0, 1)}/${filename.substring(1, 2)}/${filename}")
                    val trashes = File("users/${token.userId}/trashes/music/${directoryName}")
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
                            action = eventName,
                            value = filename,
                            appType = "music"
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