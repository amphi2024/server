package com.amphi.handlers.cloud

import com.amphi.*
import com.amphi.handlers.handleAuth
import com.amphi.utils.contentTypeByExtension
import com.amphi.utils.generateThumbnail
import com.amphi.utils.getFileExtension
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun fileDirectoryPathById(userId: String, id: String): String {
    return "users/$userId/cloud/files/${id[0]}/${id[1]}/${id[2]}/$id"
}

private fun fileDirectoryById(userId: String, id: String): File {
    return File(fileDirectoryPathById(userId, id))
}

fun createFile(req: HttpServerRequest) {
    val requestToken = req.headers()["Authorization"]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        req.bodyHandler { buffer ->
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = { sendAuthFailed(req) },
                onAuthenticated = { token ->
                    val database = CloudAppDatabase(token.userId)
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
            )
        }
    }
}

fun uploadFile(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[3]

    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        req.isExpectMultipart = true
        req.uploadHandler { upload ->
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = { sendAuthFailed(req) },
                onAuthenticated = { token ->
                    val directory = fileDirectoryById(token.userId, id)
                    val database = CloudAppDatabase(token.userId)
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
            )
        }
        req.exceptionHandler { sendUploadFailed(req) }
    }
}

fun getFiles(req: HttpServerRequest) {
    val requestToken = req.headers()["Authorization"]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = { sendAuthFailed(req) },
            onAuthenticated = { token ->
                val jsonArray = JsonArray()
                val database = CloudAppDatabase(token.userId)
                val list = database.getFiles(null)
                list.forEach { item ->
                    jsonArray.add(item.toJsonObject())
                }
                database.close()
                req.response()
                    .putHeader("content-type", "application/json; charset=UTF-8")
                    .end(jsonArray.encode())
            }
        )
    }
}

fun downloadFileInfo(req: HttpServerRequest, split: List<String>) {
    handleAuth(req) { token ->
        val id = split[3]
        val database = CloudAppDatabase(token.userId)
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
    handleAuth(req) { token ->
        req.bodyHandler { buffer ->
            val id = split[3]
            val database = CloudAppDatabase(token.userId)
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
    val requestToken = req.headers()["Authorization"]
    val id = split[3]
    val version = 1

    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = { sendAuthFailed(req) },
            onAuthenticated = { token ->
                val directoryPath = fileDirectoryPathById(token.userId, id)
                val database = CloudAppDatabase(token.userId)
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
        )
    }
}

fun deleteFile(req: HttpServerRequest, split: List<String>) {
    handleAuth(req) { token ->
        val id = split[3]
        val database = CloudAppDatabase(token.userId)
        val fileModel = database.getLatestFileModelById(id)
        if (fileModel != null) {
            database.deleteFile(fileModel)
        }

        database.close()

        val file = fileDirectoryById(token.userId, id)
        val trash = File("users/${token.userId}/trashes/cloud/files")
        if (!trash.exists()) {
            trash.mkdirs()
        }
        if (file.exists()) {
            Files.move(
                file.toPath(),
                Paths.get("${trash.path}/${id}"),
                StandardCopyOption.REPLACE_EXISTING
            )
            ServerDatabase.notifyFileDelete("${trash.path}/${id}")
            req.response().end(Messages.SUCCESS)
        } else {
            req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
        }

    }
}