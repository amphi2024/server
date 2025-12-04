package com.amphi.server.routes

import com.amphi.server.handlers.EventHandler
import com.amphi.server.handlers.MusicHandler
import com.amphi.server.handlers.WebsocketHandler
import com.amphi.server.common.sendNotFound
import com.amphi.server.handlers.ThemeHandler
import io.vertx.core.http.HttpServerRequest

object MusicRouter {

    private val websocketHandler = WebsocketHandler()

    fun route(req: HttpServerRequest) {

        val split = req.path().split("/")
        when (split.size) {
//            6 -> { //    ex: /music/songs/{song1}/files/{file.mp3}
//                when(req.method().name().uppercase()) {
//                    "GET" -> {
//                        when (split[2]) {
//                            "songs" -> {
//                                if(split[4] == "files") {
//                                    MusicHandler.downloadFile(req, split, "songs")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "artists" -> {
//                                if(split[4] == "images") {
//                                    MusicHandler.downloadFile(req, split, "artists")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "albums" -> {
//                                if(split[4] == "covers") {
//                                    MusicHandler.downloadFile(req, split, "albums")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "playlists" -> {
//                                if(split[4] == "thumbnails") {
//                                    MusicHandler.downloadFile(req, split, "playlists")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            else -> sendNotFound(req)
//                        }
//                    }
//                    "POST" -> {
//                        when (split[2]) {
//                            "songs" -> MusicHandler.uploadFile(req, split, "songs")
//                            "artists" -> MusicHandler.uploadFile(req, split, "artists")
//                            "albums" -> MusicHandler.uploadFile(req, split, "albums")
//                            "playlists" -> MusicHandler.uploadFile(req, split, "playlists")
//                            else -> sendNotFound(req)
//                        }
//                    }
//                    else -> sendNotFound(req)
//                }
//            }
//            5 -> {  //    ex: /music/songs/{song1}/files, /music/songs/{song1}/{file.mp3}
//                when(req.method().name().uppercase()) {
//                    "GET" -> {
//                        when (split[2]) {
//                            "songs" -> {
//                                if(split[4] == "files") {
//                                    MusicHandler.getMediaFiles(req, split, "songs")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "artists" -> {
//                                if(split[4] == "images") {
//                                    MusicHandler.getMediaFiles(req, split, "artists")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "albums" -> {
//                                if(split[4] == "covers") {
//                                    MusicHandler.getMediaFiles(req, split, "albums")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            "playlists" -> {
//                                if(split[4] == "thumbnails") {
//                                    MusicHandler.getMediaFiles(req, split, "playlists")
//                                }
//                                else {
//                                    sendNotFound(req)
//                                }
//                            }
//                            else -> sendNotFound(req)
//                        }
//                    }
//                    else -> sendNotFound(req)
//                }
//            }
            4 -> {   //   ex :   /music/songs/{song1}  ,   /music/themes/{theme1}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "songs" ->  MusicHandler.getSong(req, split)
                            "artists" ->  MusicHandler.getArtist(req, split)
                            "albums" -> MusicHandler.getAlbum(req, split)
                            "playlists" -> MusicHandler.getPlaylist(req, split)
                            "themes" -> MusicHandler.downloadTheme(req, split)
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicHandler.uploadSong(req, split)
                            "artists" ->  MusicHandler.uploadArtist(req, split)
                            "albums" -> MusicHandler.uploadAlbum(req, split)
                            "playlists" -> MusicHandler.uploadPlaylist(req, split)
                            "themes" ->  MusicHandler.uploadTheme(req, split)
                            else -> sendNotFound(req)
                        }
                    }
                    "DELETE" -> {
                        when(split[2]) {
                            "songs" -> MusicHandler.deleteSong(req, split)
                            "artists" ->  MusicHandler.deleteArtist(req, split)
                            "albums" -> MusicHandler.deleteAlbum(req, split)
                            "playlists" -> MusicHandler.deletePlaylist(req, split)
                            "themes" -> MusicHandler.deleteTheme(req, split)
                            else -> sendNotFound(req)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            3 -> {  //    ex: /music/themes
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (req.path()) {
                            "/music/colors" -> ThemeHandler.getColors(req, "music")
                            "/music/events" -> EventHandler.getEvents(req, "music")
                            "/music/themes" -> MusicHandler.getThemes(req)
                            "/music/sync" -> websocketHandler.handleWebsocket(req)
                            "/music/songs" -> MusicHandler.getSongs(req)
                            "/music/artists" -> MusicHandler.getArtists(req)
                            "/music/albums" -> MusicHandler.getAlbums(req)
                            "/music/playlists" -> MusicHandler.getPlaylists(req)
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        if (req.path() == "/music/colors") ThemeHandler.uploadColors(req, "music")
                        else sendNotFound(req)
                    }
                    "DELETE" -> {
                        if(req.path() == "/music/events") {
                            EventHandler.acknowledgeEvent(req)
                        }
                        else {
                            sendNotFound(req)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            2 -> {   //   ex: /music
                when(req.method().name().uppercase()) {
                    else -> sendNotFound(req)
                }
            }
            else -> {
                sendNotFound(req)
            }
        }
    }
    
}