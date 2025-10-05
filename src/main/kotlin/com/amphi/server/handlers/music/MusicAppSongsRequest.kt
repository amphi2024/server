package com.amphi.server.handlers.music

import com.amphi.server.ServerDatabase
import com.amphi.server.sendAuthFailed
import com.amphi.server.sendNotFound
import com.amphi.server.sendSuccess
import io.vertx.core.http.HttpServerRequest
import java.io.File

object MusicAppSongsRequest : MusicAppRequest {

    fun getSongs(req: HttpServerRequest) {
        getItems(req, "songs")
    }

    // /music/songs/{song}/files
    fun getFilesOfSong(req: HttpServerRequest, split: List<String>) {
        getFilesOfSomething(req, split, "songs")
    }

    // /music/songs/my-song/my-file.json
    // /music/songs/my-song/my-file.mp3
    fun uploadSongFile(req: HttpServerRequest, split: List<String>) {
        val filename = split[4]
        if(filename.endsWith(".json")) {
            val requestToken = req.headers()["Authorization"]
            val id = split[3]
            if(requestToken.isNullOrBlank()) {
                sendAuthFailed(req)
            }
            else {
                req.bodyHandler { buffer->
                    ServerDatabase.authenticateByToken(
                        token = requestToken,
                        onFailed = {
                            sendAuthFailed(req)
                        },
                        onAuthenticated = { token ->
                            val directory =  item(token, id, "songs")

                            val file = File("${directory.path}/${filename}")
                            file.writeText(buffer.toString())
                            ServerDatabase.saveEvent(token = token, action = "upload_song_file", value = id, appType = "music")

                            sendSuccess(req)
                        }
                    )
                }
            }
        }
        else {
            uploadFile(req, split, "songs","upload_song_file")
        }
    }

    // /music/songs/my-song
    fun getSongInfo(req: HttpServerRequest, split: List<String>) {
        val requestToken = req.headers()["Authorization"]
        val songId = split[3]
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
                try {
                    val directory = item(token, songId, "songs")
                    val infoFile = File("${directory.path}/info.json")
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
                }
                catch (e: Exception) {
                    sendNotFound(req)
                }
                }
            )
        }
    }

    // /music/songs/my-song
    fun uploadSongInfo(req: HttpServerRequest, split: List<String>) {
        uploadInfo(req, split, "songs", "upload_song_info")
    }

    fun downloadSongFile(req: HttpServerRequest, split: List<String>) {
        downloadFile(req, split, "songs")
    }

    fun deleteSong(req: HttpServerRequest, split: List<String>) {
        delete(req, split, "songs", "delete_song")
    }

}