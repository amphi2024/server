package com.amphi.server.routes

import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendNotFound
import com.amphi.server.handlers.EventHandler
import com.amphi.server.handlers.PhotosHandler
import com.amphi.server.handlers.ThemeHandler
import com.amphi.server.handlers.WebsocketHandler
import io.vertx.core.http.HttpServerRequest

object PhotosRouter {

    private val websocketHandler = WebsocketHandler()

    fun route(req: HttpServerRequest) {
        val split = req.path().split("/")
        when(split.size) {
            4 -> {  //    /photos/{my-photo}/info
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "albums" -> PhotosHandler.downloadAlbum(req, split)
                            "themes" -> PhotosHandler.downloadTheme(req, split)
                            else -> {
                                if(split[3] == "info") {
                                    PhotosHandler.downloadPhotoInfo(req, split)
                                }
                                else if(split[3] == "thumbnail") {
                                    PhotosHandler.downloadThumbnail(req, split)
                                }
                                else if(split[3] == "sha256") {
                                    PhotosHandler.getSha256(req, split)
                                }
                                else {
                                    sendBadRequest(req)
                                }
                            }
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "albums" -> PhotosHandler.uploadAlbum(req, split)
                            "themes" -> PhotosHandler.uploadTheme(req, split)
                            else -> {
                                if(split[3] == "info") {
                                    PhotosHandler.uploadPhotoInfo(req, split)
                                }
                                else {
                                    sendBadRequest(req)
                                }
                            }
                        }
                    }
                    "DELETE" -> {
                        when (split[2]) {
                            "albums" -> PhotosHandler.deleteAlbum(req, split)
                            "themes" -> PhotosHandler.deleteTheme(req, split)
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
                            "/photos/albums" -> PhotosHandler.getAlbums(req)
                            "/photos/colors" -> ThemeHandler.getColors(req, "photos")
                            "/photos/events" -> EventHandler.getEvents(req, "photos")
                            "/photos/themes" -> PhotosHandler.getThemes(req)
                            "/photos/sync" -> websocketHandler.handleWebsocket(req)
                            else -> PhotosHandler.downloadPhoto(req, split)
                        }
                    }
                    "POST" -> {
                        PhotosHandler.uploadPhoto(req, split)
                    }
                    "DELETE" -> {
                        if(req.path() == "/photos/events") {
                            EventHandler.acknowledgeEvent(req)
                        }
                        else {
                            PhotosHandler.deletePhoto(req, split)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            2 -> { // /photos
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        PhotosHandler.getPhotos(req)
                    }
                    else -> sendNotFound(req)
                }
            }
            else -> sendNotFound(req)
        }
    }
}