package com.amphi.handlers.music

import com.amphi.handlers.ServerEventHandler
import com.amphi.sendBadRequest
import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {

        val split = req.path().split("/")
        when (split.size) {
            5 -> {  //    ex: /music/songs/{song1}/{file1}
                when(req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[2]) {
                            "songs" -> {
                                if(split[4] == "files") {
                                    MusicAppSongsRequest.getFilesOfSong(req, split)
                                }
                                else {

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
                            "themes" -> MusicAppThemeRequest.downloadTheme(req, split)
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        when (split[2]) {
                            "songs" ->  MusicAppSongsRequest.uploadSongInfo(req, split)
                            "artists" ->  MusicAppArtistsRequest.uploadArtistInfo(req, split)
                            "albums" -> MusicAppAlbumsRequest.uploadAlbumInfo(req, split)
                            "playlists" -> MusicAppPlaylistsRequest.uploadPlaylist(req, split)
                            "themes" ->  MusicAppThemeRequest.uploadTheme(req, split)
                            else -> sendNotFound(req)
                        }
                    }
                    "DELETE" -> {
                        when(split[2]) {
                            "songs" -> MusicAppSongsRequest.deleteSong(req, split)
                            "artists" ->  MusicAppArtistsRequest.deleteArtist(req, split)
                            "albums" -> MusicAppAlbumsRequest.deleteAlbum(req, split)
                            "playlists" -> MusicAppPlaylistsRequest.deletePlaylist(req, split)
                            "themes" -> MusicAppThemeRequest.deleteTheme(req, split)
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
                            "/music/colors" -> MusicAppColorRequest.getColors(req)
                            "/music/events" -> ServerEventHandler.handleGetEvents(req, "music")
                            "/music/themes" -> MusicAppThemeRequest.getThemes(req)
                            "/music/sync" -> MusicWebSocketHandler.handleWebsocket(req)
                            "/music/songs" -> MusicAppSongsRequest.getSongs(req)
                            "/music/artists" -> MusicAppArtistsRequest.getArtists(req)
                            "/music/albums" -> MusicAppAlbumsRequest.getAlbums(req)
                            "/music/playlists" -> MusicAppPlaylistsRequest.getPlaylists(req)
                            else -> sendNotFound(req)
                        }
                    }
                    "POST" -> {
                        if (req.path() == "/music/colors") MusicAppColorRequest.uploadColors(req)
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