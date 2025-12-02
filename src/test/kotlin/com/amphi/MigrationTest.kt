package com.amphi

import com.amphi.server.utils.migration.migrateMusic
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.random.Random

class MigrationTest {

    @Test
    fun `should migrate music correctly in version 3_0_0`() {

        val userDir = File("users/user1")

        try {
            val musicDir = File(userDir, "music")
            musicDir.mkdirs()

            val songsDir = File(musicDir, "songs")
            val albumsDir = File(musicDir, "albums")
            val artistsDir = File(musicDir, "artists")
            val playlistsDir = File(musicDir, "playlists")
            val themesDir = File(musicDir, "themes")
            themesDir.mkdirs()

            createSampleMusicData(songsDir = songsDir, albumsDir = albumsDir, artistsDir = artistsDir, playlistsDir = playlistsDir)

            migrateMusic(userDir)

        } finally {
            userDir.deleteRecursively()
        }
    }

    private fun generatedId(ids: MutableSet<String>): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        while (true) {
            val length = Random.nextInt(5) + 30
            val id = (1..length)
                .map { chars.random() }
                .joinToString("")

            if (!ids.contains(id)) {
                return id
            }
        }
    }

    private fun createSampleMusicData(songsDir: File, albumsDir: File, artistsDir: File, playlistsDir: File) {
        val songIdList = mutableSetOf<String>()
        val artistIdList = mutableSetOf<String>()
        val albumIdList = mutableSetOf<String>()
        val playlistIdList = mutableSetOf<String>()
        playlistsDir.mkdirs()

        (0..10).forEach { _ ->
            val nowMilli = Instant.now().toEpochMilli()
            val id = generatedId(artistIdList)
            artistIdList.add(id)
            createSampleArtist(
                artistsDir = artistsDir,
                id = id,
                name = mapOf(
                    "default" to "Artist $id"
                ),
                added = nowMilli,
                modified = nowMilli
            )
        }

        for(i in 0..10) {
            val nowMilli = Instant.now().toEpochMilli()
            val id = generatedId(albumIdList)
            albumIdList.add(id)
            createSampleAlbum(
                albumsDir = albumsDir,
                id = id,
                title = mapOf(
                    "default" to "Album $id"
                ),
                genre = mapOf(
                    "default" to "Rock"
                ),
                artistId = if(i % 2 == 0) artistIdList.elementAt(i) else null,
                added = nowMilli,
                modified = nowMilli
            )
        }

        (0..10).forEach { i ->
            val nowMilli = Instant.now().toEpochMilli()
            val id = generatedId(songIdList)
            songIdList.add(id)
            createSampleSong(
                songsDir = songsDir,
                id = id,
                title = mapOf(
                    "default" to "Awesome Song $id"
                ),
                genres = setOf(
                    mapOf(
                        "default" to "Pop"
                    )
                ),
                artistId = if(i % 2 == 0) artistIdList.elementAt(i) else null,
                albumId = if(i % 2 == 0) albumIdList.elementAt(i) else null,
                composerId = if(i % 2 == 0) artistIdList.elementAt(i) else null,
                added = nowMilli,
                modified = nowMilli,
                released = if(i % 2 == 0) nowMilli else null,
                trackNumber = if(i % 2 == 0) i else null,
                discNumber = if(i % 2 == 0) i else null,
                archived = if(i % 2 == 0) true else null
            )
        }

        (0..3).forEach { _ ->
            val id = generatedId(playlistIdList)
            createSamplePlaylist(playlistsDir = playlistsDir, id = id, title = "Playlist $id", songs = songIdList)
        }
    }

    private fun Map<String, String>.toJsonObject() : JsonObject {
        val jsonObject = JsonObject()
        this.forEach { (key, value) ->
            jsonObject.put(key, value)
        }
        return jsonObject
    }

    private fun Set<Map<String, String>>.toJsonArray() : JsonArray {
        val jsonArray = JsonArray()
        forEach { item ->
            jsonArray.add(item.toJsonObject())
        }
        return jsonArray
    }

    private fun createSampleSong(songsDir: File, id: String, title: Map<String, String>, genres: Set<Map<String, String>>, artistId: String?, albumId: String?, composerId: String?, added: Long, modified: Long, released: Long?, trackNumber: Int?, discNumber: Int?, archived: Boolean?) {
        val directory = File(songsDir, "${id[0]}/${id[1]}/$id")
        directory.mkdirs()
        val infoFile = File(directory, "info.json")

        val jsonObject = JsonObject()

        jsonObject.put("title", title.toJsonObject())

        jsonObject.put("genre", genres.toJsonArray())

        jsonObject.put("artist", artistId)
        jsonObject.put("album", albumId)
        jsonObject.put("added", added)
        jsonObject.put("modified", modified)
        jsonObject.put("composer", composerId)
        jsonObject.put("released", released)
        jsonObject.put("trackNumber", trackNumber)
        jsonObject.put("discNumber", discNumber)
        jsonObject.put("archived", archived)

        infoFile.writeText(jsonObject.toString())

        val songInfoFile = File(directory, "SONG_FILE_ID.json")
        songInfoFile.writeText("""
            {"format":"mp3","lyrics":{"default":[{"startsAt":0,"endsAt":0,"text":""}]}}
        """.trimIndent())
        val songFile = File(directory, "SONG_FILE_ID.mp3")
        songFile.writeBytes(byteArrayOf(10))

        val songInfoFile1 = File(directory, "SONG_FILE_ID_1.json")
        songInfoFile1.writeText("""
            {"format":"flac","lyrics":{"default":[{"startsAt":0,"endsAt":0,"text":"Hi"}]}}
        """.trimIndent())
        val songFile1 = File(directory, "SONG_FILE_ID_1.flac")
        songFile1.writeBytes(byteArrayOf(10))
    }

    private fun createSampleAlbum(albumsDir: File, id: String, title: Map<String, String>, genre: Map<String, String>, artistId: String?, added: Long, modified: Long) {
        val directory = File(albumsDir, "${id[0]}/${id[1]}/$id")
        directory.mkdirs()
        val infoFile = File(directory, "info.json")
        val jsonObject = JsonObject()
        jsonObject.put("title", title.toJsonObject())
        jsonObject.put("genre", genre.toJsonObject())
        jsonObject.put("artist", artistId)
        jsonObject.put("added", added)
        jsonObject.put("modified", modified)

        infoFile.writeText(jsonObject.toString())

        val coverFile = File(directory, "COVER_ID.jpg")
        coverFile.writeBytes(byteArrayOf(10))
    }

    private fun createSampleArtist(artistsDir: File, id: String, name: Map<String, String>, added: Long, modified: Long) {
        val directory = File(artistsDir, "${id[0]}/${id[1]}/$id")
        directory.mkdirs()
        val infoFile = File(directory, "info.json")
        val jsonObject = JsonObject()
        jsonObject.put("name", name.toJsonObject())
        jsonObject.put("added", added)
        jsonObject.put("modified", modified)

        infoFile.writeText(jsonObject.toString())

        val profileImage = File(directory, "IMAGE_ID.jpg")
        profileImage.writeBytes(byteArrayOf(10))
    }

    private fun createSamplePlaylist(playlistsDir: File, id: String, title: String, songs: MutableSet<String>) {
        val file = File(playlistsDir, "$id.playlist")
        val jsonObject = JsonObject()
        jsonObject.put("title", title)
        val jsonArray = JsonArray()
        songs.forEach { songId ->
            jsonArray.add(songId)
        }
        jsonObject.put("songs", jsonArray)

        file.writeText(jsonObject.toString())
    }
}
