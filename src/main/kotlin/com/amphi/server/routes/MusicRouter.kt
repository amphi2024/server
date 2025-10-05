package com.amphi.server.routes

import com.amphi.server.handlers.EventHandler
import com.amphi.server.handlers.MusicHandler
import com.amphi.server.handlers.ThemeHandler
import com.amphi.server.handlers.WebsocketHandler
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicRouter {

    private val websocketHandler = WebsocketHandler()

    fun route(req: HttpServerRequest) {

        val split = req.path().split("/")
        when (split.size) {
            5 -> {  //    ex: /music/songs/{song1}/files, /music/songs/{song1}/{file.mp3}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "songs" -> {
                                if(split[4] == "files") {
                                    MusicHandler.getFilesOfSomething(req, split, "songs")
                                }
                                else {
                                    MusicHandler.downloadFile(req, split, "songs")
                                }
                            }
                            "artists" -> {
                                if(split[4] == "files") {
                                    MusicHandler.getFilesOfSomething(req, split, "artists")
                                }
                                else {
                                    MusicHandler.downloadFile(req, split, "artists")
                                }
                            }
                            "albums" -> {
                                if(split[4] == "covers") {
                                    MusicHandler.getAlbumCovers(req, split)
                                }
                                else {
                                    MusicHandler.downloadFile(req, split, "albums")
                                }
                            }
                            "playlists" -> {
                                if(split[4] == "thumbnails") {
                                    MusicHandler.getPlaylistThumbnails(req, split)
                                }
                                else {
                                    MusicHandler.downloadPlaylistThumbnail(req, split)
                                }
                            }
                            else -> sendBadRequest(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicHandler.uploadSongFile(req, split)
                            "artists" ->  MusicHandler.uploadFile(req, split, "artists", "upload_artist_file")
                            "albums" ->  MusicHandler.uploadFile(req, split, "albums", "upload_album_cover")
                            "playlists" ->  MusicHandler.uploadPlaylistThumbnail(req, split)
                            else -> sendBadRequest(req)
                        }
                    }
                    "DELETE" -> {
                        when (split[2]) {
                            else -> sendBadRequest(req)
                        }
                    }
                    else -> sendNotFound(req)
                }
            }
            4 -> {   //   ex :   /music/songs/{song1}  ,   /music/themes/{theme1}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "songs" ->  MusicHandler.getSongInfo(req, split)
                            "artists" ->  MusicHandler.getInfo(req, split, "artists")
                            "albums" -> MusicHandler.getAlbumInfo(req, split)
                            "playlists" -> MusicHandler.getPlaylist(req, split)
                            "themes" -> ThemeHandler.downloadTheme(req, "music", split[3])
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicHandler.uploadInfo(req, split, "songs", "upload_song_info")
                            "artists" ->  MusicHandler.uploadInfo(req, split, "artists", "upload_artist_info")
                            "albums" -> MusicHandler.uploadInfo(req, split, "albums", "upload_album_info")
                            "playlists" -> MusicHandler.uploadPlaylist(req, split)
                            "themes" ->  ThemeHandler.uploadTheme(req, "music", split[3])
                            else -> sendNotFound(req)
                        }
                    }
                    "DELETE" -> {
                        when(split[2]) {
                            "songs" -> MusicHandler.delete(req, split, "songs", "delete_song")
                            "artists" ->  MusicHandler.delete(req, split, "artists", "delete_artist")
                            "albums" -> MusicHandler.delete(req, split, "albums" ,"delete_album")
                            "playlists" -> MusicHandler.deletePlaylist(req, split)
                            "themes" -> ThemeHandler.deleteTheme(req, "music", split[3])
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
                            "/music/themes" -> ThemeHandler.getThemes(req, "music")
                            "/music/sync" -> websocketHandler.handleWebsocket(req)
                            "/music/songs" -> MusicHandler.getItems(req, "songs")
                            "/music/artists" -> MusicHandler.getItems(req, "artists")
                            "/music/albums" -> MusicHandler.getItems(req, "albums")
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