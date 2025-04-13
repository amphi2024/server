package com.amphi.handlers.music

import com.amphi.*
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object MusicAppPlaylistsRequest {

    fun getPlaylists(req: HttpServerRequest) {
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
                    val directory = File("users/${token.userId}/music/playlists")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    directory.listFiles()?.forEach { playlistFile ->
                        val jsonObject = JsonObject()
                        jsonObject.put("id", playlistFile.nameWithoutExtension)
                        jsonObject.put("filename", playlistFile.name)
                        jsonObject.put("modified", playlistFile.lastModified())
                       jsonArray.add(jsonObject)
                    }
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
                }
            )
        }
    }

    fun getPlaylist(req: HttpServerRequest, split: List<String>) {
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
                    val file = File("users/${token.userId}/music/playlists/$id.playlist")
                    try {
                        req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
                    }
                    catch (e: Exception) {
                        sendNotFound(req)
                    }
                }
            )
        }
    }

    fun getPlaylistThumbnails(req: HttpServerRequest, split: List<String>) {
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
                    val directory = File("users/${token.userId}/music/playlists/$id")
                    directory.listFiles()?.forEach { file ->
                        val jsonObject = JsonObject()
                        jsonObject.put("filename", file.name)
                        jsonObject.put("modified", file.lastModified())
                        jsonArray.add(jsonObject)
                    }
                    try {
                        req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
                    }
                    catch (e: Exception) {
                        sendNotFound(req)
                    }
                }
            )
        }
    }

    fun uploadPlaylist(req: HttpServerRequest, split: List<String>) {
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
                        val file = File("users/${token.userId}/music/playlists/$id.playlist")
                        file.writeText(buffer.toString())
                        ServerDatabase.saveEvent(token = token, action = "upload_playlist", value = id, appType = "music")

                        sendSuccess(req)
                    }
                )
            }
        }
    }

    fun uploadPlaylistThumbnail(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
        val filename = split[5]
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
                        val file = File("users/${token.userId}/music/playlists/$id/$filename")

                        upload.streamToFileSystem(file.path).onComplete { ar ->
                            if (ar.succeeded()) {
                                ServerDatabase.saveEvent(token = token, action = "upload_playlist_thumbnail", value = filename, appType = "music")
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

    fun downloadPlaylistThumbnail(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val id = split[3]
        val filename = split[5]
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
                    val filePath = "users/${token.userId}/music/playlists/$id/$filename"
                    if (!File(filePath).exists()) {
                        sendFileNotExists(req)
                    } else {
                        req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
                    }
                }
            )
        }
    }

    fun deletePlaylist(req: HttpServerRequest, split: List<String>) {
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
                    val file = File("users/${token.userId}/music/playlists/${filename}.playlist")
                    val trashes = File("users/${token.userId}/trashes/music/playlists")
                    if (!trashes.exists()) {
                        trashes.mkdirs()
                    }
                    if (file.exists()) {
                        Files.move(
                            file.toPath(),
                            Paths.get("${trashes.path}/${filename}.playlist"),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        ServerDatabase.notifyFileDelete("${trashes.path}/${filename}.playlist")
                        ServerDatabase.saveEvent(
                            token = token,
                            action = "delete_playlist",
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