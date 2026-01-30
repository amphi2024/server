package com.amphi

import com.amphi.server.configs.AppConfig
import com.amphi.server.trashService
import com.amphi.server.utils.migration.migratePhotos
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random

class PhotosMigrationTest {

    @Test
    fun `should migrate photos correctly in version 3_0_0`() {
        val userDir = File("${AppConfig.storage.data}/user1")

        try {
            val photosDir = File(userDir, "photos")
            val libraryDir = File(photosDir, "library")
            val albumsDir = File(photosDir, "albums")
            libraryDir.mkdirs()
            albumsDir.mkdirs()

            createSampleData(libraryDir, albumsDir)
            migratePhotos("user1", userDir)

        } finally {
            userDir.deleteRecursively()
            trashService.getTrashLogs().forEach { trashLog ->
                if(trashLog.path.startsWith("${AppConfig.storage.data}/user1")) {
                    trashService.deleteTrashLog(trashLog.path)
                }
            }
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

    private fun createSampleData(photosDir: File, albumsDir: File) {
        val photoIds = mutableSetOf<String>()
        val albumIds = mutableSetOf<String>()
        val jsonArray = JsonArray()

        for(i in 0..10) {
            val id = generatedId(photoIds)
            photoIds.add(id)
            if(i < 7) {
                jsonArray.add(id)
            }
            createSamplePhoto(
                photosDir = photosDir,
                id = id,
                deleted = if(i == 3) 4 else null,
                isError = i == 8
            )
        }

        for(i in 0..10) {
            val id = generatedId(albumIds)
            albumIds.add(id)

            createSampleAlbum(
                albumsDir = albumsDir,
                id = id,
                photos = jsonArray,
                isError = i == 5
            )
        }
    }

    private fun createSamplePhoto(photosDir: File, id: String, deleted: Long? = null, isError: Boolean = false) {
        val directory = File(photosDir, "${id[0]}/${id[1]}/$id")
        directory.mkdirs()
        val infoFile = File(directory, "info.json")
        val jsonObject = JsonObject()
        jsonObject.put("title", "Photo $id")
        jsonObject.put("created", 1)
        jsonObject.put("modified", 2)
        jsonObject.put("date", 3)
        jsonObject.put("deleted", deleted)
        jsonObject.put("mimeType", "image/jpg")
        jsonObject.put("sha256", "SHA-256:)")

        infoFile.writeText(jsonObject.toString())
        if(isError) {
            infoFile.writeText("$jsonObject}}}}")
        }

        val photoFile = File(directory, "photo.jpg")
        photoFile.writeBytes(byteArrayOf(10))
    }

    private fun createSampleAlbum(albumsDir: File, id: String, photos: JsonArray, isError: Boolean = false) {
        val file = File(albumsDir, "${id}.album")
        val jsonObject = JsonObject()
        jsonObject.put("title", "Album $id")
        jsonObject.put("created", 8)
        jsonObject.put("modified", 9)
        jsonObject.put("photos", photos)

        file.writeText(jsonObject.toString())
        if(isError) {
            file.writeText("$jsonObject}}}}")
        }
    }
}