package com.amphi.server.handlers

import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendNotFound
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.configs.AppConfig
import com.amphi.server.eventService
import com.amphi.server.models.photos.Album
import com.amphi.server.models.photos.Photo
import com.amphi.server.models.photos.PhotosDatabase
import com.amphi.server.models.photos.PhotosTheme
import com.amphi.server.models.photos.PhotosThemeColors
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.utils.generateImageThumbnail
import com.amphi.server.utils.generateMultiResVideo
import com.amphi.server.utils.generateVideoThumbnail
import com.amphi.server.utils.getNullableInt
import com.amphi.server.utils.getNullableJsonArray
import com.amphi.server.utils.getNullableLong
import com.amphi.server.utils.getNullableString
import com.amphi.server.utils.isVideoExtension
import com.amphi.server.utils.moveToTrash
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.security.MessageDigest

object PhotosHandler {

    fun uploadAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val jsonObject = buffer.toJsonObject()
                val database = PhotosDatabase(token.userId)
                val album = Album(
                    id = id,
                    title = jsonObject.getString("title"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    deleted = jsonObject.getNullableLong("deleted"),
                    photos = jsonObject.getJsonArray("photos"),
                    coverPhotoIndex = jsonObject.getNullableInt("cover_photo_index"),
                    note = jsonObject.getNullableString("note")
                )
                database.insertAlbum(album)
                eventService.saveEvent(
                    token = token,
                    action = "upload_album",
                    value = id,
                    appType = "photos"
                )

                sendSuccess(req)
                database.close()
            }
        }
    }

    fun downloadAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = PhotosDatabase(token.userId)
            val album = database.getAlbumById(id)
            if (album == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8")
                    .end(album.toJsonObject().toString())
            }
        }
    }

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) { token ->
            val database = PhotosDatabase(token.userId)
            database.setAlbumDeleted(id)
            eventService.saveEvent(
                token = token,
                action = "delete_album",
                value = id,
                appType = "photos"
            )
            sendSuccess(req)
            database.close()
        }
    }

    fun getAlbums(req: HttpServerRequest) {
        handleAuthorization(req) { token ->
            val database = PhotosDatabase(token.userId)
            val jsonArray = JsonArray()
            database.getAlbums().forEach { album ->
                jsonArray.add(album.id)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            database.close()
        }
    }

    private fun photoDirectoryPathById(userId: String, id: String): String {
        return "${AppConfig.storage.data}/${userId}/photos/library/${id[0]}/${id[1]}/$id"
    }

    private fun photoDirectoryById(userId: String, id: String): File {
        val directory = File(photoDirectoryPathById(userId, id))
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun photoFileById(path: String): File? {
        val directory = File(path)
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.name.startsWith("photo")) {
                    return file
                }
            }
            return null
        } else {
            return null
        }
    }

    fun uploadPhotoInfo(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val jsonObject = buffer.toJsonObject()
                val database = PhotosDatabase(token.userId)

                val photo = Photo(
                    id = id,
                    title = jsonObject.getString("title"),
                    created = jsonObject.getLong("created"),
                    modified = jsonObject.getLong("modified"),
                    date = jsonObject.getLong("date"),
                    deleted = jsonObject.getNullableLong("deleted"),
                    mimeType = jsonObject.getNullableString("mime_type") ?: jsonObject.getString("mimeType"),
                    sha256 = jsonObject.getString("sha256"),
                    note = jsonObject.getNullableString("note"),
                    tags = jsonObject.getNullableJsonArray("tags"),
                )

                database.insertPhoto(photo)

                eventService.saveEvent(token = token, action = "upload_photo", value = id, appType = "photos")
                sendSuccess(req)
            }
        }
    }

    fun uploadPhoto(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            req.isExpectMultipart = true
            val database = PhotosDatabase(token.userId)
            val photo = database.getPhotoById(id)
            if (photo == null) {
                sendBadRequest(req)
                return@handleAuthorization
            }
            val fileExtension = photo.mimeType.split("/").last()
            database.close()
            req.uploadHandler { upload ->
                val directory = photoDirectoryById(token.userId, id)
                val photoFilePath = "${directory.path}/photo.$fileExtension"

                upload.streamToFileSystem(photoFilePath).onComplete { ar ->
                    if (ar.succeeded()) {
                        if (isVideoExtension(fileExtension)) {
                            if (AppConfig.media.generateThumbnail) {
                                generateVideoThumbnail(
                                    input = photoFilePath,
                                    output = "${directory.path}/thumbnail.jpg"
                                )
                            }
                            if (AppConfig.media.multiResVideo) {
                                val output1080p = "${directory.path}/photo_1080p.$fileExtension"
                                val output720p = "${directory.path}/photo_720p.$fileExtension"
                                generateMultiResVideo(
                                    filepath = photoFilePath,
                                    outputPath1080p = output1080p,
                                    outputPath720p = output720p
                                )
                            }

                        }

                        if (AppConfig.media.generateThumbnail) {
                            generateImageThumbnail(
                                input = photoFilePath,
                                output = "${directory.path}/thumbnail.jpg"
                            )
                        }
                        sendSuccess(req)
                    } else {
                        sendUploadFailed(req)
                    }
                }
            }
            req.exceptionHandler { _ ->
                sendUploadFailed(req)
            }
        }
    }

    fun downloadPhotoInfo(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val database = PhotosDatabase(token.userId)
            val photo = database.getPhotoById(id)
            if (photo == null) {
                sendNotFound(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8")
                    .end(photo.toJsonObject().toString())
            }
        }
    }

    fun downloadThumbnail(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = File("$directoryPath/thumbnail.jpg")
            if (file.exists()) {
                req.response().putHeader("content-type", "image/jpeg").sendFile(file.path)
            } else {
                if (AppConfig.media.generateThumbnail) {
                    photoFileById(directoryPath)?.let { photoFile ->
                        if (isVideoExtension(photoFile.extension)) {
                            generateVideoThumbnail(photoFile.path, file.path)
                        } else {
                            generateImageThumbnail(photoFile.path, file.path)
                        }
                    }
                }
                sendNotFound(req)
            }
        }
    }

    fun downloadPhoto(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = photoFileById(directoryPath)
            if (file != null) {
                req.response().putHeader("content-type", contentTypeByExtension(file.extension)).sendFile(file.path)
            } else {
                sendFileNotExists(req)
            }
        }
    }

    fun deletePhoto(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val directory = photoDirectoryById(token.userId, id)
            val database = PhotosDatabase(token.userId)
            val photo = database.getPhotoById(id)
            if(photo == null) {
                sendBadRequest(req)
                return@handleAuthorization
            }
            if (directory.exists()) {
                database.setPhotoDeleted(id)
                directory.listFiles()?.forEach { file ->
                    moveToTrash(
                        userId = token.userId,
                        path = "photos/library/${id[0]}/${id[1]}/id",
                        filename = file.name
                    )
                }
                eventService.saveEvent(
                    token = token,
                    action = "delete_photo",
                    value = id,
                    appType = "photos"
                )
                sendSuccess(req)
            } else {
                sendFileNotExists(req)
            }
            database.close()
        }
    }

    fun getPhotos(req: HttpServerRequest) {
        handleAuthorization(req) { token ->
            val database = PhotosDatabase(token.userId)
            val jsonArray = JsonArray()
            database.getPhotos().forEach { photo ->
                jsonArray.add(photo.id)
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            database.close()
        }
    }

    fun getSha256(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) { token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = photoFileById(directoryPath)
            if (file != null) {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { fis ->
                    val buffer = ByteArray(1024 * 4)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                req.response().putHeader("content-type", "text/plain")
                    .end(digest.digest().joinToString("") { "%02x".format(it) })
            } else {
                sendFileNotExists(req)
            }
        }
    }

    fun getThemes(req: HttpServerRequest) {
        handleAuthorization(req) { token ->

            val database = PhotosDatabase(token.userId)
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
                val database = PhotosDatabase(token.userId)
                val jsonObject = buffer.toJsonObject()
                if(id.endsWith(".theme")) {
                    sendBadRequest(req)
                    return@bodyHandler
                }
                val theme = PhotosTheme(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    modified = jsonObject.getLong("modified"),
                    created = jsonObject.getLong("created"),
                    lightColors = PhotosThemeColors(
                        background = jsonObject.getLong("background_light"),
                        text = jsonObject.getLong("text_light"),
                        accent = jsonObject.getLong("accent_light"),
                        card = jsonObject.getLong("card_light")
                    ),
                    darkColors = PhotosThemeColors(
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
            val database = PhotosDatabase(token.userId)
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
            val database = PhotosDatabase(token.userId)
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