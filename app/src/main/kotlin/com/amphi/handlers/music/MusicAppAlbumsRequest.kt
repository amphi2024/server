package com.amphi.handlers.music

import com.amphi.*
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File

object MusicAppAlbumsRequest : MusicAppRequest {

    fun getAlbums(req: HttpServerRequest) {
        getItems(req, "albums")
    }

    fun getAlbumCovers(req: HttpServerRequest, split: List<String>) {
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
                    val directory =  File("users/${token.userId}/music/albums/${id.substring(0, 1)}/${id.substring(1, 2)}/$id/covers")
                    if(!directory.exists()) {
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
            )
        }
    }

    fun getAlbumInfo(req: HttpServerRequest, split: List<String>) {
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
                    val directory = item(token, id, "albums")
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

    fun uploadAlbumInfo(req: HttpServerRequest, split: List<String>) {
        uploadInfo(req, split, "albums", "upload_album_info")
    }

    fun uploadAlbumCover(req: HttpServerRequest, split: List<String>) {
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
                        val directory = item(token, id, "albums")
                        val coversDir = File("${directory.path}/covers")
                        if(!coversDir.exists()) {
                            coversDir.mkdirs()
                        }
                        upload.streamToFileSystem("${directory.path}/covers/${filename}").onComplete { ar ->
                            if (ar.succeeded()) {
                                ServerDatabase.saveEvent(token = token, action = "upload_album_cover", value = filename, appType = "music")
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

    fun downloadAlbumCover(req: HttpServerRequest, split: List<String>) {
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
                    val filePath = "users/${token.userId}/music/albums/${id.substring(0, 1)}/${id.substring(1, 2)}/${id}/covers/${filename}"
                    if (!File(filePath).exists()) {
                        sendFileNotExists(req)
                    } else {
                        req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
                    }
                }
            )
        }
    }

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        delete(req, split, "albums" ,"delete_album")
    }

}