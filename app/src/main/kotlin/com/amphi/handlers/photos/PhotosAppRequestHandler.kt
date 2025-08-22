package com.amphi.handlers.photos

import com.amphi.ServerDatabase
import com.amphi.handlers.ServerEventHandler
import com.amphi.handlers.ThemeHandler
import com.amphi.sendAuthFailed
import com.amphi.sendBadRequest
import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File

object PhotosAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {

        val split = req.path().split("/")
        when(split.size) {
            4 -> {  //    /photos/{my-photo}/info
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "albums" -> downloadAlbum(req, split)
                            else -> {
                                if(split[3] == "info") {
                                    downloadPhotoInfo(req, split)
                                }
                                else if(split[3] == "thumbnail") {
                                    downloadThumbnail(req, split)
                                }
                                else if(split[3] == "sha256") {
                                    getSha256(req, split)
                                }
                                else {
                                    sendBadRequest(req)
                                }
                            }
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "albums" -> uploadAlbum(req, split)
                            else -> {
                                if(split[3] == "info") {
                                    uploadPhotoInfo(req, split)
                                }
                                else {
                                    sendBadRequest(req)
                                }
                            }
                        }
                    }
                    "DELETE" -> {
                        when (split[2]) {
                            "albums" -> deleteAlbum(req, split)
                            else -> sendBadRequest(req)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            3 -> { // /photos/{my-photo}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (req.path()) {
                            "/photos/albums" -> getAlbums(req)
                            "/photos/colors" -> ThemeHandler.getColors(req, "photos")
                            "/photos/events" -> ServerEventHandler.handleGetEvents(req, "photos")
                            "/photos/themes" -> ThemeHandler.getThemes(req, "photos")
                            "/photos/sync" -> PhotosWebSocketHandler.handleWebsocket(req)
                            else -> downloadPhoto(req, split)
                        }
                    }
                    "POST" -> {
                        uploadPhoto(req, split)
                    }
                    "DELETE" -> {
                        if(req.path() == "/photos/events") {
                            ServerEventHandler.handleAcknowledgeEvent(req)
                        }
                        else {
                            deletePhoto(req, split)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            2 -> { // /photos
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        getPhotos(req)
                    }
                    else -> sendNotFound(req)
                }
            }
            else -> sendNotFound(req)
        }
    }
}