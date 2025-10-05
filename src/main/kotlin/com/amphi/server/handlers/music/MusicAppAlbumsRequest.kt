package com.amphi.server.handlers.music

import com.amphi.server.ServerDatabase
import com.amphi.server.sendAuthFailed
import com.amphi.server.sendNotFound
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
                    val directory =  File("users/${token.userId}/music/albums/${id.substring(0, 1)}/${id.substring(1, 2)}/$id")
                    if(!directory.exists()) {
                        directory.mkdirs()
                    }
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
        uploadFile(req, split, "albums", "upload_album_cover")
    }

    fun downloadAlbumCover(req: HttpServerRequest, split: List<String>) {
        downloadFile(req, split, "albums")
    }

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        delete(req, split, "albums" ,"delete_album")
    }

}