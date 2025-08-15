package com.amphi.handlers.photos

import com.amphi.Messages
import com.amphi.ServerDatabase
import com.amphi.sendAuthFailed
import com.amphi.sendNotFound
import com.amphi.sendSuccess
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun uploadAlbum(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[3]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        req.bodyHandler { buffer ->
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = { sendAuthFailed(req) },
                onAuthenticated = { token ->
                    val directory = File("users/${token.userId}/photos/albums")
                    if(!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File("users/${token.userId}/photos/albums/${id}.album")
                    file.writeText(buffer.toString())
                    ServerDatabase.saveEvent(
                        token = token,
                        action = "upload_album",
                        value = id,
                        appType = "photos"
                    )

                    sendSuccess(req)
                }
            )
        }
    }
}

fun downloadAlbum(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[3]
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
                    val file = File("users/${token.userId}/photos/albums/$id.album")
                    req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
                } catch (_: Exception) {
                    sendNotFound(req)
                }
            }
        )
    }
}

fun deleteAlbum(req: HttpServerRequest, split: List<String>) {
    val requestToken = req.headers()["Authorization"]
    val id = split[3]
    if (requestToken.isNullOrBlank()) {
        sendAuthFailed(req)
    } else {
        ServerDatabase.authenticateByToken(
            token = requestToken,
            onFailed = {
                sendAuthFailed(req)
            },
            onAuthenticated = { token ->
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
                    ServerDatabase.notifyFileDelete("${trashes.path}/${id}.album")
                    ServerDatabase.saveEvent(
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
        )
    }
}

fun getAlbums(req: HttpServerRequest) {
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
                val directory = File("users/${token.userId}/photos/albums")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                directory.listFiles()?.forEach { file ->
                    jsonArray.add(file.name.split(".").first())
                }
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
            }
        )
    }
}