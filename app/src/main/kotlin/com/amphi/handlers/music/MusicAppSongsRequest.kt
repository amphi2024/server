package com.amphi.handlers.music

import com.amphi.*
import com.amphi.models.Token
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

object MusicAppSongsRequest : MusicAppRequest {

    fun getSongs(req: HttpServerRequest) {
        getItems(req, "songs")
    }

    // /music/songs/{song}/files
    fun getFilesOfSong(req: HttpServerRequest, split: List<String>) {
        getFilesOfSomething(req, split, "songs")
    }

    // /music/songs/my-song/my-file.lyrics
    // /music/songs/my-song/my-file.mp3
    fun uploadSongFile(req: HttpServerRequest, split: List<String>) {
        uploadFile(req, split, "songs","upload_song_file")
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
                    //val jsonObject = songInfo(token, songId)
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
                }
                catch (e: Exception) {
                    sendNotFound(req)
                }
                }
            )
        }
    }

    private fun songInfo(token: Token, songId: String) : JsonObject {
        val directory = item(token, songId, "songs")
        val infoFile = File("${directory.path}/info.json")
        val fileContent = infoFile.readText()
        val jsonObject = JsonObject(fileContent)

        if(jsonObject.getJsonObject("title").getValue("default") == null) {
            directory.listFiles()?.forEach { file ->
                when(file.extension) {
                    "mp3", "flac", "m4a", "wav", "ogg", "aac" -> {
                        val tag = AudioFileIO.read(file).tag
                        tag.getFirst(FieldKey.TITLE)?.let { title ->
                            jsonObject.getJsonObject("title").put("default", title)
                            infoFile.writeText(jsonObject.toString())
                        }

                        jsonObject.getString("artist")?.let { artistId ->
                            val artistDirectory = item(token, artistId, "artists")
                            val artistInfoFile = File("${artistDirectory.path}/info.json")

                            val artistInfo = JsonObject(artistInfoFile.readText())
                            if(artistInfo.getJsonObject("title").getValue("default") == null) {
                                tag.getFirst("artist")?.let { artistName ->
                                    artistInfo.getJsonObject("title").put("default", artistName)
                                }
                            }
                        }
                    }
                }
            }
        }

        return jsonObject
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