package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendNotFound
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.configs.ServerSettings
import com.amphi.server.eventService
import com.amphi.server.trashService
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.utils.generateImageThumbnail
import com.amphi.server.utils.generateMultiResVideo
import com.amphi.server.utils.generateVideoThumbnail
import com.amphi.server.utils.isVideoExtension
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object PhotosHandler {

    fun uploadAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val directory = File("users/${token.userId}/photos/albums")
                if(!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File("users/${token.userId}/photos/albums/${id}.album")
                file.writeText(buffer.toString())
                eventService.saveEvent(
                    token = token,
                    action = "upload_album",
                    value = id,
                    appType = "photos"
                )

                sendSuccess(req)
            }
        }
    }

    fun downloadAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            try {
                val file = File("users/${token.userId}/photos/albums/$id.album")
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
            } catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            val file = File("users/${token.userId}/photos/albums/$id.album")
            val trashes = File("users/${token.userId}/trashes/photos/albums")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${id}.album"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                trashService.notifyFileDelete("${trashes.path}/${id}.album")
                eventService.saveEvent(
                    token = token,
                    action = "delete_album",
                    value = id,
                    appType = "photos"
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }
        }
    }

    fun getAlbums(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/photos/albums")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.listFiles()?.forEach { file ->
                jsonArray.add(file.name.split(".").first())
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    private fun photoDirectoryPathById(userId: String, id: String): String {
        return "users/${userId}/photos/library/${id[0]}/${id[1]}/$id"
    }

    private fun photoDirectoryById(userId: String, id: String): File {
        val directory = File(photoDirectoryPathById(userId, id))
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun photoFileById(path: String) : File? {
        val directory = File(path)
        val files = directory.listFiles()
        if(files != null) {
            for (file in files) {
                if (file.name.startsWith("photo")) {
                    return file
                }
            }
            return null
        }
        else {
            return null
        }
    }

    fun uploadPhotoInfo(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val directory = photoDirectoryById(token.userId, id)

                val file = File("${directory.path}/info.json")
                file.writeText(buffer.toString())
                eventService.saveEvent(token = token, action = "upload_photo", value = id, appType = "photos")

                sendSuccess(req)
            }
        }
    }

    fun uploadPhoto(req: HttpServerRequest, split: List<String>) {
        val fileExtension = req.headers()["X-File-Extension"]
        val id = split[2]
        handleAuthorization(req) {token ->
            req.isExpectMultipart = true
            if(fileExtension == null) {
                sendBadRequest(req)
            }
            else {
                req.uploadHandler { upload ->
                    val directory = photoDirectoryById(token.userId, id)
                    val photoFilePath = "${directory.path}/photo.$fileExtension"

                    upload.streamToFileSystem(photoFilePath).onComplete { ar ->
                        if (ar.succeeded()) {
                            if(isVideoExtension(fileExtension)) {
                                if(ServerSettings.generateMediaThumbnail) {
                                    generateVideoThumbnail(
                                        input = photoFilePath,
                                        output = "${directory.path}/thumbnail.jpg"
                                    )
                                }
                                if( ServerSettings.multiResVideo) {
                                    val output1080p = "${directory.path}/photo_1080p.$fileExtension"
                                    val output720p = "${directory.path}/photo_720p.$fileExtension"
                                    generateMultiResVideo(
                                        filepath = photoFilePath,
                                        outputPath1080p = output1080p,
                                        outputPath720p = output720p
                                    )
                                }

                            }

                            if(ServerSettings.generateMediaThumbnail) {
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
    }

    fun downloadPhotoInfo(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) {token ->
            try {
                val directory = photoDirectoryById(token.userId, id)
                val infoFile = File("${directory.path}/info.json")
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
            } catch (_: Exception) {
                sendNotFound(req)
            }
        }
    }

    fun downloadThumbnail(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) {token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = File("$directoryPath/thumbnail.jpg")
            if(file.exists()){
                req.response().putHeader("content-type", "image/jpeg").sendFile(file.path)
            }
            else {
                if(ServerSettings.generateMediaThumbnail) {
                    photoFileById(directoryPath)?.let { photoFile ->
                        if(isVideoExtension(photoFile.extension)) {
                            generateVideoThumbnail(photoFile.path, file.path)
                        }
                        else {
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
        handleAuthorization(req) {token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = photoFileById(directoryPath)
            if(file != null) {
                req.response().putHeader("content-type", contentTypeByExtension(file.extension)).sendFile(file.path)
            }
            else {
                sendFileNotExists(req)
            }
        }
    }

    fun deletePhoto(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) {token ->
            val file = photoDirectoryById(token.userId, id)
            val trashes = File("users/${token.userId}/trashes/photos/library")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${id}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                trashService.notifyFileDelete("${trashes.path}/${id}")
                eventService.saveEvent(
                    token = token,
                    action = "delete_photo",
                    value = id,
                    appType = "photos"
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }
        }
    }

    fun getPhotos(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/photos/library")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.listFiles()?.forEach { subDir ->  //    /photos/{directoryName}/a
                subDir.listFiles()?.forEach { subDir2 -> //    /photos/{directoryName}/a/b
                    subDir2.listFiles()?.forEach { file -> //    /photos/{directoryName}/a/b/{abPhoto}
                        jsonArray.add(file.name)
                    }
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun getSha256(req: HttpServerRequest, split: List<String>) {
        val id = split[2]
        handleAuthorization(req) {token ->
            val directoryPath = photoDirectoryPathById(token.userId, id)
            val file = photoFileById(directoryPath)
            if(file != null) {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { fis ->
                    val buffer = ByteArray(1024 * 4)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                req.response().putHeader("content-type", "text/plain").end(digest.digest().joinToString("") { "%02x".format(it) })
            }
            else {
                sendFileNotExists(req)
            }
        }
    }

}