package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.common.StatusCode
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendNotFound
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.sendUploadFailed
import com.amphi.server.configs.ServerSettings
import com.amphi.server.models.CloudDatabase
import com.amphi.server.models.FileModel
import com.amphi.server.trashService
import com.amphi.server.utils.contentTypeByExtension
import com.amphi.server.utils.generateThumbnail
import com.amphi.server.utils.getFileExtension
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object CloudHandler {

    fun fileDirectoryPathById(userId: String, id: String): String {
        return "users/$userId/cloud/files/${id[0]}/${id[1]}/${id[2]}/$id"
    }

    private fun fileDirectoryById(userId: String, id: String): File {
        return File(fileDirectoryPathById(userId, id))
    }

    fun createFile(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            req.bodyHandler { buffer ->
                val cloudDirectory = File("users/${token.userId}/cloud")
                if(!cloudDirectory.exists()) {
                    cloudDirectory.mkdirs()
                }
                val database = CloudDatabase(token.userId)
                val id = database.generateUniqueFileId()
                val directoryPath = fileDirectoryPathById(token.userId, id)
                val file = File("$directoryPath/1")
                file.mkdirs()

                try {
                    database.insertFile(
                        FileModel.fromRequestBuffer(id, buffer)
                    )
                    database.close()
                    req.response()
                        .putHeader("content-type", "text/plain")
                        .setStatusCode(StatusCode.SUCCESS)
                        .end(id)
                } catch (_: Exception) {
                    req.response()
                        .putHeader("content-type", "text/plain")
                        .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR)
                        .end(Messages.FAILED)
                }
            }
        }
    }

    fun uploadFile(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        handleAuthorization(req) {token ->
            req.isExpectMultipart = true
            req.uploadHandler { upload ->
                val directory = fileDirectoryById(token.userId, id)
                val database = CloudDatabase(token.userId)
                val fileModel = database.getLatestFileModelById(id)
                database.close()
                if (fileModel == null) {
                    sendUploadFailed(req)
                } else {
                    val fileExtension = getFileExtension(fileModel.name)
                    val filename = if (fileExtension.isEmpty()) "file" else "file.${fileExtension}"
                    val filePath = "${directory.path}/${fileModel.version}/${filename}"
                    upload.streamToFileSystem(filePath).onComplete { ar ->
                        if (ar.succeeded()) {
                            if (ServerSettings.generateMediaThumbnail) {
                                generateThumbnail(
                                    fileExtension = fileExtension,
                                    input = filePath,
                                    output = "${directory.path}/${fileModel.version}/thumbnail.jpg"
                                )
                            }
                            sendSuccess(req)
                        } else {
                            sendUploadFailed(req)
                        }
                    }
                }
            }
            req.exceptionHandler { sendUploadFailed(req) }
        }
    }

    fun getFiles(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            val jsonArray = JsonArray()
            val database = CloudDatabase(token.userId)
            val list = database.getFiles(null)
            list.forEach { item ->
                jsonArray.add(item.toJsonObject())
            }
            database.close()
            req.response()
                .putHeader("content-type", "application/json; charset=UTF-8")
                .end(jsonArray.encode())
        }
    }

    fun downloadFileInfo(req: HttpServerRequest, split: List<String>) {
        handleAuthorization(req) { token ->
            val id = split[3]
            val database = CloudDatabase(token.userId)
            val fileModel = database.getLatestFileModelById(id)
            database.close()

            if (fileModel == null) {
                sendNotFound(req)
            } else {
                req.response()
                    .putHeader("content-type", "application/json; charset=UTF-8")
                    .end(fileModel.toJsonObject().encode())
            }
        }
    }

    fun updateFileInfo(req: HttpServerRequest, split: List<String>) {
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val id = split[3]
                val database = CloudDatabase(token.userId)
                val originalFileModel = database.getLatestFileModelById(id)
                if (originalFileModel != null) {
                    val fileModel = FileModel.fromRequestBuffer(id, buffer)
                    fileModel.version = ++originalFileModel.version
                    database.insertFile(fileModel)
                    sendSuccess(req)
                } else {
                    sendBadRequest(req)
                }
                database.close()
            }
        }
    }

    fun downloadFile(req: HttpServerRequest, split: List<String>) {
        val id = split[3]
        val version = 1
        handleAuthorization(req) {token ->
            val directoryPath = fileDirectoryPathById(token.userId, id)
            val database = CloudDatabase(token.userId)
            val fileModel = database.getLatestFileModelById(id)
            database.close()
            if (fileModel == null) {
                sendNotFound(req)
            } else {
                val fileExtension = getFileExtension(fileModel.name)
                val filename = if (fileExtension.isEmpty()) "file" else "file.${fileExtension}"
                val file = File("$directoryPath/$version/${filename}")
                if (file.exists()) {
                    val contentType = contentTypeByExtension(file.extension)
                    req.response().putHeader("content-type", contentType).sendFile(file.path)
                } else {
                    sendFileNotExists(req)
                }
            }
        }
    }

    fun deleteFile(req: HttpServerRequest, split: List<String>) {
        handleAuthorization(req) { token ->
            val id = split[3]
            val database = CloudDatabase(token.userId)
            val fileModel = database.getLatestFileModelById(id)
            if (fileModel != null) {
                database.deleteFile(fileModel)
            }

            database.close()

            val file = fileDirectoryById(token.userId, id)
            val trash = File("users/${token.userId}/trash/cloud/files")
            if (!trash.exists()) {
                trash.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trash.path}/${id}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                trashService.notifyFileDelete("${trash.path}/${id}")
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }

        }
    }

}