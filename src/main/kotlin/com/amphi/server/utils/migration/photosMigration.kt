package com.amphi.server.utils.migration

import com.amphi.server.models.photos.Album
import com.amphi.server.models.photos.Photo
import com.amphi.server.models.photos.PhotosDatabase
import com.amphi.server.utils.moveToTrash
import java.io.File

fun migratePhotos(userDirectory: File) {
    val userId = userDirectory.name
    val database = PhotosDatabase(userId)
    val albumsDirectory = File("${userDirectory.path}/photos/albums")

    if(!albumsDirectory.exists()) {
        return
    }

    val libraryDirectory = File("${userDirectory.path}/photos/library")

    var migrated = true

    libraryDirectory.listFiles()?.forEach { subDir ->
        subDir.listFiles()?.forEach { subDir2 ->
            subDir2.listFiles()?.forEach { photoDirectory ->
                if(photoDirectory.isDirectory) {
                    val infoFile = File(photoDirectory, "info.json")
                    val id = photoDirectory.name
                    if(infoFile.exists()) {
                        val photo = Photo.legacy(infoFile)
                        database.insertPhoto(photo) {result ->
                            if(result > 0) {
                                moveToTrash(userId = userId, path = "photos/library/${id[0]}/${id[1]}/$id", filename = "info.json")
                            }
                            else {
                                migrated = false
                            }
                        }
                    }
                }
            }
        }
    }

    albumsDirectory.listFiles()?.forEach { file ->
        if(file.isFile) {
            val album = Album.legacy(file)
            database.insertAlbum(album) {result ->
                if(result > 0) {
                    moveToTrash(userId = userId, path = "photos/albums", filename = file.name)
                }
                else {
                    migrated = false
                }
            }
        }
    }

    if(migrated) {
        albumsDirectory.delete()
        val themesDirectory = File("${userDirectory.path}/photos/themes")
        if(themesDirectory.exists()) {
            themesDirectory.delete()
        }
    }
}