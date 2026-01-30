package com.amphi.server.handlers

import com.amphi.server.models.Token
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendNotFound
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendAuthFailed
import com.amphi.server.common.sendBadRequest
import com.amphi.server.configs.AppConfig
import com.amphi.server.eventService
import com.amphi.server.models.music.Album
import com.amphi.server.models.music.Artist
import com.amphi.server.models.music.MusicDatabase
import com.amphi.server.models.music.MusicTheme
import com.amphi.server.models.music.MusicThemeColors
import com.amphi.server.models.music.Playlist
import com.amphi.server.models.music.Song
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.utils.getNullableInt
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import com.amphi.server.utils.moveToTrash
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File

object MusicHandler {
    private fun itemDirectory(token: Token, id: String, directoryName: String) : File {
        val directory = File("${AppConfig.storage.data}/${token.userId}/music/media/${directoryName}/${id[0]}/${id[1]}/$id")
        print(directory.canonicalPath)
        if(!directory.canonicalPath.startsWith(AppConfig.storage.data)) {
            throw SecurityException()
        }
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }
    
    fun getSongs(req: HttpServerRequest) {
        handleAuthorization(req) {token -> 
            val database = MusicDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getSongs().forEach { song ->
                jsonArray.add(song.id)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            
            database.close()
        }
    }
    
    fun getArtists(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val database = MusicDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getArtists().forEach { artist ->
                jsonArray.add(artist.id)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())

            database.close()
        }
    }

    fun getAlbums(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val database = MusicDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getAlbums().forEach { album ->
                jsonArray.add(album.id)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())

            database.close()
        }
    }

    fun getMediaFiles(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = itemDirectory(token, id, directoryName)
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                        val jsonObject = JsonObject()
                        jsonObject.put("filename", file.name)
                        jsonObject.put("modified", file.lastModified())
                        jsonObject.put("size", file.length())
                        jsonArray.add(jsonObject)
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun getSong(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            val song = database.getSongById(id)
            if (song == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(song.toJsonObject().toString())
            }
        }
    }

    fun getArtist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            val artist = database.getArtistById(id)
            if (artist == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(artist.toJsonObject().toString())
            }
        }
    }

    fun getAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            val album = database.getAlbumById(id)
            if (album == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(album.toJsonObject().toString())
            }
        }
    }

    fun getPlaylist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            val playlist = database.getPlaylistById(id)
            if (playlist == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(playlist.toJsonObject().toString())
            }
        }
    }

    fun uploadSong(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = MusicDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(!jsonObject.containsKey("id")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val song = Song(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getJsonObject("title"),
                    genres = jsonObject.getNullableJsonArray("genres"),
                    artistIds = jsonObject.getNullableJsonArray("artist_ids"),
                    albumId = jsonObject.getNullableString("album_id"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    deleted = jsonObject.getLong("deleted"),
                    composerIds = jsonObject.getNullableJsonArray("composer_ids"),
                    lyricistIds = jsonObject.getNullableJsonArray("lyricist_ids"),
                    arrangerIds = jsonObject.getNullableJsonArray("arranger_ids"),
                    producerIds = jsonObject.getNullableJsonArray("producer_ids"),
                    archived = jsonObject.getBoolean("archived"),
                    released = jsonObject.getNullableLong("released"),
                    trackNumber = jsonObject.getNullableInt("track_number"),
                    discNumber = jsonObject.getNullableInt("disc_number"),
                    description = jsonObject.getNullableString("description"),
                    files = jsonObject.getJsonArray("files"),
                    featuredArtistIds = jsonObject.getNullableJsonArray("featured_artist_ids"),
                )

                database.insertSong(song)
                eventService.saveEvent(token = token, action = "upload_song", value = id, appType = "music")

                sendSuccess(req)
                database.close()
            }
        }
    }
    
    fun uploadArtist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = MusicDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(!jsonObject.containsKey("id")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val artist = Artist(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getJsonObject("name"),
                    images = jsonObject.getNullableJsonArray("images"),
                    members = jsonObject.getNullableJsonArray("members"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    deleted = jsonObject.getLong("deleted"),
                    debut = jsonObject.getNullableLong("debut"),
                    country = jsonObject.getNullableString("country"),
                    description = jsonObject.getNullableString("description")
                )

                database.insertArtist(artist)
                eventService.saveEvent(token = token, action = "upload_artist", value = id, appType = "music")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun uploadAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = MusicDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(!jsonObject.containsKey("id")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val album = Album(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getJsonObject("title"),
                    covers = jsonObject.getNullableJsonArray("covers"),
                    genres = jsonObject.getNullableJsonArray("genres"),
                    artistIds = jsonObject.getNullableJsonArray("artist_ids"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    deleted = jsonObject.getLong("deleted"),
                    released = jsonObject.getNullableLong("released"),
                    description = jsonObject.getNullableString("description")
                )

                database.insertAlbum(album)
                eventService.saveEvent(token = token, action = "upload_album", value = id, appType = "music")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]
        val filename = split[5]

        handleAuthorization(req) { token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val directory = itemDirectory(token, id, directoryName)

                upload.streamToFileSystem("${directory.path}/${filename}").onComplete { ar ->
                    if (ar.succeeded()) {
                        sendSuccess(req)
                    } else {
                        sendUploadFailed(req)
                    }
                }
            }
            req.exceptionHandler {
                sendUploadFailed(req)
            }
        }
    }

    // /music/songs/my-song/files/file.mp3
    // /music/albums/my-album/covers/my-cover.jpg
    fun downloadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]
        val filename = split[5]

        handleAuthorization(req) { token ->
            val file = File(itemDirectory(token, id, directoryName), filename)
            if (!file.exists()) {
                sendFileNotExists(req)
            }
            else if(!file.canonicalPath.startsWith(AppConfig.storage.data)) {
                sendAuthFailed(req)
            }
            else {
                req.response().putHeader("content-type", contentTypeByExtension(file.extension)).sendFile(file.path)
            }
        }
    }

    fun deleteSong(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            database.setSongDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_song",
                value = id,
                appType = "music"
            )
            val directory = itemDirectory(token = token, id = id, directoryName = "songs")
            directory.listFiles()?.forEach { file ->
                moveToTrash(userId = token.userId, path = "music/media/songs/${id[0]}/${id[1]}/$id", filename = file.name)
            }
            sendSuccess(req)
            database.close()
        }
    }

    fun deleteArtist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            database.setArtistDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_artist",
                value = id,
                appType = "music"
            )
            val directory = itemDirectory(token = token, id = id, directoryName = "artists")
            directory.listFiles()?.forEach { file ->
                moveToTrash(userId = token.userId, path = "music/media/artists/${id[0]}/${id[1]}/$id", filename = file.name)
            }
            sendSuccess(req)
            database.close()
        }
    }

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            database.setAlbumDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_album",
                value = id,
                appType = "music"
            )
            val directory = itemDirectory(token = token, id = id, directoryName = "albums")
            directory.listFiles()?.forEach { file ->
                moveToTrash(userId = token.userId, path = "music/media/albums/${id[0]}/${id[1]}/$id", filename = file.name)
            }
            sendSuccess(req)
            database.close()
        }
    }

    fun getPlaylists(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val database = MusicDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getPlaylists().forEach { playlist ->
                val jsonObject = JsonObject()
                jsonObject.put("id", playlist.id)
                jsonObject.put("modified", playlist.modified)
                jsonArray.add(jsonObject)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            database.close()
        }
    }

    fun uploadPlaylist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val database = MusicDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(!jsonObject.containsKey("id")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val playlist = Playlist(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    songs = jsonObject.getJsonArray("songs"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    deleted = jsonObject.getNullableLong("deleted"),
                    thumbnails = jsonObject.getNullableJsonArray("thumbnails"),
                    note = jsonObject.getNullableString("note")
                )
                database.insertPlaylist(playlist)

                eventService.saveEvent(token = token, action = "upload_playlist", value = id, appType = "music")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun deletePlaylist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            database.setPlaylistDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_playlist",
                value = id,
                appType = "music"
            )
            val directory = itemDirectory(token = token, id = id, directoryName = "playlists")
            directory.listFiles()?.forEach { file ->
                moveToTrash(userId = token.userId, path = "music/media/playlists/${id[0]}/${id[1]}/$id", filename = file.name)
            }
            sendSuccess(req)
            database.close()
        }
    }

    fun getThemes(req: HttpServerRequest) {
        handleAuthorization(req) { token ->

            val database = MusicDatabase(token.userId)
            val jsonArray = JsonArray()

            database.getThemes().forEach { theme ->
                jsonArray.add(theme.toJsonObject())
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())

            database.close()
        }
    }

    fun uploadTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val database = MusicDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(id.endsWith(".theme")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val theme = MusicTheme(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    modified = jsonObject.getLong("modified"),
                    created = jsonObject.getLong("created"),
                    lightColors = MusicThemeColors(
                        background = jsonObject.getLong("background_light"),
                        text = jsonObject.getLong("text_light"),
                        accent = jsonObject.getLong("accent_light"),
                        card = jsonObject.getLong("card_light")
                    ),
                    darkColors = MusicThemeColors(
                        background = jsonObject.getLong("background_dark"),
                        text = jsonObject.getLong("text_dark"),
                        accent = jsonObject.getLong("accent_dark"),
                        card = jsonObject.getLong("card_dark")
                    )
                )
                database.insertTheme(theme)
                eventService.saveEvent(token = token, action = "upload_theme", value = id, appType = "music")

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun downloadTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            val theme = database.getThemeById(id)
            if (theme == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(theme.toJsonObject().toString())
            }
            database.close()
        }
    }

    fun deleteTheme(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = MusicDatabase(token.userId)
            database.deleteTheme(id)
            eventService.saveEvent(
                token = token,
                action = "delete_theme",
                value = id,
                appType = "music"
            )
            sendSuccess(req)
            database.close()
        }
    }
}