package com.amphi.server.handlers.music

import com.amphi.server.handlers.ServerEventHandler
import com.amphi.server.handlers.ThemeHandler
import com.amphi.server.sendBadRequest
import com.amphi.server.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {

        val split = req.path().split("/")
        when (split.size) {
            5 -> {  //    ex: /music/songs/{song1}/files, /music/songs/{song1}/{file.mp3}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "songs" -> {
                                if(split[4] == "files") {
                                    MusicAppSongsRequest.getFilesOfSong(req, split)
                                }
                                else {
                                    MusicAppSongsRequest.downloadSongFile(req, split)
                                }
                            }
                            "artists" -> {
                                if(split[4] == "files") {
                                    MusicAppArtistsRequest.getArtistFiles(req, split)
                                }
                                else {
                                    MusicAppArtistsRequest.downloadArtistFile(req, split)
                                }
                            }
                            "albums" -> {
                                if(split[4] == "covers") {
                                    MusicAppAlbumsRequest.getAlbumCovers(req, split)
                                }
                                else {
                                    MusicAppAlbumsRequest.downloadAlbumCover(req, split)
                                }
                            }
                            "playlists" -> {
                                if(split[4] == "thumbnails") {
                                    MusicAppPlaylistsRequest.getPlaylistThumbnails(req, split)
                                }
                                else {
                                    MusicAppPlaylistsRequest.downloadPlaylistThumbnail(req, split)
                                }
                            }
                            else -> sendBadRequest(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicAppSongsRequest.uploadSongFile(req, split)
                            "artists" ->  MusicAppArtistsRequest.uploadArtistFile(req, split)
                            "albums" ->  MusicAppAlbumsRequest.uploadAlbumCover(req, split)
                            "playlists" ->  MusicAppPlaylistsRequest.uploadPlaylistThumbnail(req, split)
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
                            "songs" ->  MusicAppSongsRequest.getSongInfo(req, split)
                            "artists" ->  MusicAppArtistsRequest.getArtistInfo(req, split)
                            "albums" -> MusicAppAlbumsRequest.getAlbumInfo(req, split)
                            "playlists" -> MusicAppPlaylistsRequest.getPlaylist(req, split)
                            "themes" -> ThemeHandler.downloadTheme(req, "music", split[3])
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicAppSongsRequest.uploadSongInfo(req, split)
                            "artists" ->  MusicAppArtistsRequest.uploadArtistInfo(req, split)
                            "albums" -> MusicAppAlbumsRequest.uploadAlbumInfo(req, split)
                            "playlists" -> MusicAppPlaylistsRequest.uploadPlaylist(req, split)
                            "themes" ->  ThemeHandler.uploadTheme(req, "music", split[3])
                            else -> sendNotFound(req)
                        }
                    }
                    "DELETE" -> {
                        when(split[2]) {
                            "songs" -> MusicAppSongsRequest.deleteSong(req, split)
                            "artists" ->  MusicAppArtistsRequest.deleteArtist(req, split)
                            "albums" -> MusicAppAlbumsRequest.deleteAlbum(req, split)
                            "playlists" -> MusicAppPlaylistsRequest.deletePlaylist(req, split)
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
                            "/music/events" -> ServerEventHandler.handleGetEvents(req, "music")
                            "/music/themes" -> ThemeHandler.getThemes(req, "music")
                            "/music/sync" -> MusicWebSocketHandler.handleWebsocket(req)
                            "/music/songs" -> MusicAppSongsRequest.getSongs(req)
                            "/music/artists" -> MusicAppArtistsRequest.getArtists(req)
                            "/music/albums" -> MusicAppAlbumsRequest.getAlbums(req)
                            "/music/playlists" -> MusicAppPlaylistsRequest.getPlaylists(req)
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        if (req.path() == "/music/colors") ThemeHandler.uploadColors(req, "music")
                        else sendNotFound(req)
                    }
                    "DELETE" -> {
                        if(req.path() == "/music/events") {
                            ServerEventHandler.handleAcknowledgeEvent(req)
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