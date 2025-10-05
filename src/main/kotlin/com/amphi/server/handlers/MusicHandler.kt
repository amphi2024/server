package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.ServerDatabase

import com.amphi.server.models.Token
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendNotFound
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.common.handleAuthorization
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object MusicHandler {
    fun item(token: Token, id: String, directoryName: String) : File {
        val directory = File("users/${token.userId}/music/${directoryName}/${id[0]}/${id[1]}/$id")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun getItems(req: HttpServerRequest, directoryName: String) {
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/music/${directoryName}")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.listFiles()?.forEach { subDir ->  //    /music/songs/a
                subDir.listFiles()?.forEach { subDir2 -> //    /music/songs/a/b
                    subDir2.listFiles()?.forEach { file -> //    /music/songs/a/b/{abMusic}
                        jsonArray.add(file.name)
                    }
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun getFilesOfSomething(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = item(token, id, directoryName)
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
    }

    fun getInfo(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]

        handleAuthorization(req) { token ->
            val directory = item(token, id, directoryName)
            val infoFile = File("${directory.path}/info.json")
            try {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
            }
            catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun uploadInfo(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val directory =  item(token, id, directoryName)

                val file = File("${directory.path}/info.json")
                file.writeText(buffer.toString())
                ServerDatabase.saveEvent(token = token, action = eventName, value = id, appType = "music")

                sendSuccess(req)
            }
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val id = split[3]
        val filename = split[4]

        handleAuthorization(req) { token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val directory = item(token, id, directoryName)

                upload.streamToFileSystem("${directory.path}/${filename}").onComplete { ar ->
                    if (ar.succeeded()) {
                        ServerDatabase.saveEvent(token = token, action = eventName, value = "$id;$filename", appType = "music")
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

    // /music/songs/my-song/my-file.lyrics
    // /music/albums/my-album/my-cover.jpg
    fun downloadFile(req: HttpServerRequest, split: List<String>, directoryName: String) {
        val id = split[3]
        val filename = split[4]

        handleAuthorization(req) { token ->
            val filePath = "users/${token.userId}/music/${directoryName}/${id[0]}/${id[1]}/${id}/${filename}"
            val file = File(filePath)
            if (!file.exists()) {
                sendFileNotExists(req)
            } else {
                val contentType = when(file.extension) {
                    "mp3" -> "audio/mpeg"
                    "aac", "m4a" -> "audio/aac"
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    "ogg" -> "audio/ogg"
                    "opus" -> "audio/opus"
                    "amr" -> "audio/amr"
                    "weba" -> "audio/webm"
                    "mp4" -> "video/mp4"
                    "mkv" -> "video/x-matroska"
                    "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"
                    "mov" -> "video/quicktime"
                    "wmv" -> "video/x-ms-wmv"
                    "flv" -> "video/x-flv"
                    "3gp" -> "video/3gpp"
                    "3g2" -> "video/3gpp2"
                    else -> "application/octet-stream"
                }
                req.response().putHeader("content-type", contentType).sendFile(filePath)
            }
        }
    }


    // /music/songs/my-song/my-file.lyrics
    // /music/songs/my-song/my-file.mp3
    fun delete(req: HttpServerRequest, split: List<String>, directoryName: String, eventName: String) {
        val filename = split[3]

        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/music/${directoryName}/${filename[0]}/${filename[1]}/${filename}")
            val trashes = File("users/${token.userId}/trashes/music/${directoryName}")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${filename}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                ServerDatabase.notifyFileDelete("${trashes.path}/${filename}")
                ServerDatabase.saveEvent(
                    token = token,
                    action = eventName,
                    value = filename,
                    appType = "music"
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }

        }
    }

    fun getAlbumCovers(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory =  File("users/${token.userId}/music/albums/${id[0]}/${id[1]}/$id")
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
    }

    fun getAlbumInfo(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val directory = item(token, id, "albums")
            val infoFile = File("${directory.path}/info.json")
            try {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
            }
            catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun getPlaylists(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/music/playlists")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.listFiles()?.forEach { playlistFile ->
                val jsonObject = JsonObject()
                jsonObject.put("id", playlistFile.nameWithoutExtension)
                jsonObject.put("filename", playlistFile.name)
                jsonObject.put("modified", playlistFile.lastModified())
                jsonArray.add(jsonObject)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun getPlaylist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            val file = File("users/${token.userId}/music/playlists/$id.playlist")
            try {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
            }
            catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun getPlaylistThumbnails(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/music/playlists/$id")
            directory.listFiles()?.forEach { file ->
                val jsonObject = JsonObject()
                jsonObject.put("filename", file.name)
                jsonObject.put("modified", file.lastModified())
                jsonArray.add(jsonObject)
            }
            try {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            }
            catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun uploadPlaylist(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val file = File("users/${token.userId}/music/playlists/$id.playlist")
                file.writeText(buffer.toString())
                ServerDatabase.saveEvent(token = token, action = "upload_playlist", value = id, appType = "music")

                sendSuccess(req)
            }
        }
    }

    fun uploadPlaylistThumbnail(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        val filename = split[4]
        handleAuthorization(req) {token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val file = File("users/${token.userId}/music/playlists/$id/$filename")

                upload.streamToFileSystem(file.path).onComplete { ar ->
                    if (ar.succeeded()) {
                        ServerDatabase.saveEvent(token = token, action = "upload_playlist_thumbnail", value = filename, appType = "music")
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

    fun downloadPlaylistThumbnail(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        val filename = split[4]
        handleAuthorization(req) {token ->
            val filePath = "users/${token.userId}/music/playlists/$id/$filename"
            if (!File(filePath).exists()) {
                sendFileNotExists(req)
            } else {
                req.response().putHeader("content-type", "application/octet-stream").sendFile(filePath)
            }
        }
    }

    fun deletePlaylist(req: HttpServerRequest, split: List<String>) {
        val filename = split[3]
        handleAuthorization(req) {token ->
            val file = File("users/${token.userId}/music/playlists/${filename}.playlist")
            val trashes = File("users/${token.userId}/trashes/music/playlists")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${filename}.playlist"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                ServerDatabase.notifyFileDelete("${trashes.path}/${filename}.playlist")
                ServerDatabase.saveEvent(
                    token = token,
                    action = "delete_playlist",
                    value = filename,
                    appType = "music"
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }
        }
    }

    // /music/songs/my-song/my-file.json
    // /music/songs/my-song/my-file.mp3
    fun uploadSongFile(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        val filename = split[4]
        handleAuthorization(req) {token ->
            if(filename.endsWith(".json")) {
                req.bodyHandler { buffer ->
                    val directory =  item(token, id, "songs")

                    val file = File("${directory.path}/${filename}")
                    file.writeText(buffer.toString())
                    ServerDatabase.saveEvent(token = token, action = "upload_song_file", value = id, appType = "music")

                    sendSuccess(req)
                }
            }
            else {
                uploadFile(req, split, "songs","upload_song_file")
            }

        }
    }

    // /music/songs/my-song
    fun getSongInfo(req: HttpServerRequest, split: List<String>) {
        val songId = split[3]
        handleAuthorization(req) {token ->
            try {
                val directory = item(token, songId, "songs")
                val infoFile = File("${directory.path}/info.json")
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
            }
            catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }
}