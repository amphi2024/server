package com.amphi.server.utils.migration

import com.amphi.server.models.music.Album
import com.amphi.server.models.music.Artist
import com.amphi.server.models.music.MusicDatabase
import com.amphi.server.models.music.Playlist
import com.amphi.server.models.music.Song
import com.amphi.server.utils.moveToTrash
import java.io.File

fun migrateMusic(userDirectory: File) {
    val userId = userDirectory.name
    val songsDirectory = File("${userDirectory.path}/music/songs")
    if(!songsDirectory.exists()) {
        return
    }
    val database = MusicDatabase(userId)
    val artistsDirectory = File("${userDirectory.path}/music/artists")
    val albumsDirectory = File("${userDirectory.path}/music/albums")
    val playlistsDirectory = File("${userDirectory.path}/music/playlists")

    var migrated = true

    songsDirectory.listFiles()?.forEach { subDir -> //    /music/songs/a
        subDir.listFiles()?.forEach { subDir2 -> //    /music/songs/a/b
            subDir2.listFiles()?.forEach { directory -> //    /music/songs/a/b/{abMusic}
                val infoFile = File(directory, "info.json")
                val id = directory.name
                if(infoFile.exists()) {
                    val song = Song.legacy(infoFile)
                    database.insertSong(song) { result ->
                        if(result > 0) {
                            directory.listFiles()?.forEach { file ->
                                if(file.extension == "json") {
                                    moveToTrash(userId = userId, path = "music/songs/${id[0]}/${id[1]}/$id", filename = file.name)
                                }
                            }
                        }
                        else {
                            migrated = false
                        }
                    }
                }
            }
        }
    }

    artistsDirectory.listFiles()?.forEach { subDir -> //    /music/artists/a
        subDir.listFiles()?.forEach { subDir2 -> //    /music/artists/a/b
            subDir2.listFiles()?.forEach { directory -> //    /music/artists/a/b/{abArtist}
                val infoFile = File(directory, "info.json")
                val id = directory.name
                if(infoFile.exists()) {
                    val artist = Artist.legacy(infoFile)
                    database.insertArtist(artist) { result ->
                        if(result > 0) {
                            moveToTrash(userId = userId, path = "music/artists/${id[0]}/${id[1]}/$id", filename = infoFile.name)
                        }
                        else {
                            migrated = false
                        }
                    }
                }
            }
        }
    }

    albumsDirectory.listFiles()?.forEach { subDir -> //    /music/albums/a
        subDir.listFiles()?.forEach { subDir2 -> //    /music/albums/a/b
            subDir2.listFiles()?.forEach { directory -> //    /music/albums/a/b/{abAlbum}
                val infoFile = File(directory, "info.json")
                val id = directory.name
                if(infoFile.exists()) {
                    val album = Album.legacy(infoFile)
                    database.insertAlbum(album) { result ->
                        if(result > 0) {
                            moveToTrash(userId = userId, path = "music/albums/${id[0]}/${id[1]}/$id", filename = infoFile.name)
                        }
                        else {
                            migrated = false
                        }

                    }
                }
            }
        }
    }

    playlistsDirectory.listFiles()?.forEach { file ->
        if(file.isFile) {
            val playlist = Playlist.legacy(file)
            database.insertPlaylist(playlist) { result ->
                if(result > 0) {
                    moveToTrash(userId = userId, path = "music/playlists", filename = file.name)
                }
                else {
                    migrated = false
                }
            }
        }
    }
    database.close()

    if(migrated) {
        val mediaDirectory = File("${userDirectory.path}/music/media")
        if(!mediaDirectory.exists()) {
            mediaDirectory.mkdirs()
        }
        songsDirectory.renameTo( File("${userDirectory.path}/music/media/songs"))
        artistsDirectory.renameTo( File("${userDirectory.path}/music/media/artists"))
        albumsDirectory.renameTo( File("${userDirectory.path}/music/media/albums"))
        playlistsDirectory.renameTo( File("${userDirectory.path}/music/media/playlists"))

        val themesDirectory = File("${userDirectory.path}/music/themes")
        if(themesDirectory.exists()) {
            themesDirectory.delete()
        }
    }
}