package com.amphi.handlers.music

import com.amphi.*
import io.vertx.core.http.HttpServerRequest
import java.io.File

object MusicAppAlbumsRequest : MusicAppRequest {

    fun getAlbums(req: HttpServerRequest) {
        sendNotFound(req)
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
                        req.response().putHeader("content-type", "application/json").end(infoFile.readText())
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

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        delete(req, split, "albums" ,"delete_album")
    }

}