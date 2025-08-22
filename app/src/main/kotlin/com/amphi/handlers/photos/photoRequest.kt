package com.amphi.handlers.photos

import com.amphi.*
import com.amphi.utils.*
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.concurrent.thread

private fun photoDirectoryPathById(userId: String, id: String): String {
    return "users/${userId}/photos/library/${id.substring(0, 1)}/${id.substring(1, 2)}/$id"
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
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    print(id)
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        req.bodyHandler { buffer ->

            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val directory = photoDirectoryById(token.userId, id)

                    val file = File("${directory.path}/info.json")
                    file.writeText(buffer.toString())
                    ServerDatabase.saveEvent(token = token, action = "upload_photo", value = id, appType = "photos")

                    sendSuccess(req)
                }
            )
        }
    }
}

fun uploadPhoto(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val fileExtension = req.headers()["X-File-Extension"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        req.isExpectMultipart = true
        if(fileExtension == null) {
            sendBadRequest(req)
        }
        else {
            req.uploadHandler { upload ->

                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                        sendAuthFailed(req)
                    },
                    onAuthenticated = { token ->
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
                                        thread {
                                            try {
                                                val output1080p = "${directory.path}/photo_1080p.$fileExtension"
                                                val output720p = "${directory.path}/photo_720p.$fileExtension"

                                                val resolution = getVideoResolution(photoFilePath)
                                                if (resolution != null) {
                                                    val (width, height) = resolution

                                                    if (width >= 3840 || height >= 2160) {
                                                        convertVideo(photoFilePath, output1080p, 1920, 1080)
                                                        convertVideo(photoFilePath, output720p, 1280, 720)
                                                    } else if (width >= 1920 || height >= 1080) {
                                                        convertVideo(photoFilePath, output1080p, 1920, 1080)
                                                        convertVideo(photoFilePath, output720p, 1280, 720)
                                                    }
                                                }


                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }.start()
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
                )

            }
            req.exceptionHandler { e ->
                sendUploadFailed(req)
            }
        }
    }
}

fun downloadPhotoInfo(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
                try {
                    val directory = photoDirectoryById(token.userId, id)
                    val infoFile = File("${directory.path}/info.json")
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(infoFile.readText())
                } catch (_: Exception) {
                    sendNotFound(req)
                }
            }
        )
    }
}

fun downloadThumbnail(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
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
        )
    }
}

fun downloadPhoto(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
                val directoryPath = photoDirectoryPathById(token.userId, id)
                val file = photoFileById(directoryPath)
                if(file != null) {
                    val contentType = when(file.extension) {
                        "webp" -> "image/webp"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "bmp" -> "image/bmp"
                        "tiff", "tif" -> "image/tiff"
                        "svg" -> "image/svg+xml"
                        "ico" -> "image/vnd.microsoft.icon"
                        "heic" -> "image/heic"
                        "heif" -> "image/heif"
                        "jfif" -> "image/jpeg"
                        "pjpeg" -> "image/pjpeg"
                        "pjp" -> "image/jpeg"
                        "avif" -> "image/avif"

                        "raw" -> "image/x-raw"
                        "dng" -> "image/x-adobe-dng"
                        "cr2" -> "image/x-canon-cr2"
                        "nef" -> "image/x-nikon-nef"
                        "arw" -> "image/x-sony-arw"
                        "rw2" -> "image/x-panasonic-rw2"
                        "orf" -> "image/x-olympus-orf"
                        "sr2" -> "image/x-sony-sr2"
                        "raf" -> "image/x-fuji-raf"
                        "pef" -> "image/x-pentax-pef"

                        "mp4" -> "video/mp4"
                        "mov" -> "video/quicktime"
                        "avi" -> "video/x-msvideo"
                        "wmv" -> "video/x-ms-wmv"
                        "mkv" -> "video/x-matroska"
                        "flv" -> "video/x-flv"
                        "webm" -> "video/webm"
                        "mpeg", "mpg" -> "video/mpeg"
                        "m4v" -> "video/x-m4v"
                        "3gp" -> "video/3gpp"
                        "3g2" -> "video/3gpp2"
                        "f4v" -> "video/x-f4v"
                        "swf" -> "application/x-shockwave-flash"
                        "vob" -> "video/dvd"
                        "ts" -> "video/mp2t"
                        else -> "application/octet-stream"
                    }
                    req.response().putHeader("content-type", contentType).sendFile(file.path)
                }
                else {
                    sendFileNotExists(req)
                }
            }
        )
    }
}

fun deletePhoto(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
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
                    ServerDatabase.notifyFileDelete("${trashes.path}/${id}")
                    ServerDatabase.saveEvent(
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
        )
    }
}

fun getPhotos(req: HttpServerRequest) {
    val requestToken = req.headers()["Authorization"]
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
        )
    }
}

fun getSha256(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[2]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
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
        )
    }
}