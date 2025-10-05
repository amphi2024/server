package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.ServerDatabase
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.handleAuthorization
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ThemeHandler {

    fun getThemes(req: HttpServerRequest, appType: String) {
        handleAuthorization(req) { token ->
            val jsonArray = JsonArray()
            val directory = File("users/${token.userId}/${appType}/themes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    val jsonObject = JsonObject()
                    jsonObject.put("filename", file.name)
                    jsonObject.put("modified", file.lastModified())
                    jsonArray.add(jsonObject)
                }
            }
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(jsonArray.encode())
        }
    }

    fun uploadTheme(req: HttpServerRequest, appType: String , filename: String) {
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val file = File("users/${token.userId}/${appType}/themes/${filename}")
                file.writeText(buffer.toString())
                ServerDatabase.saveEvent(token = token, action = "upload_theme", value = filename, appType = appType)

                sendSuccess(req)
            }
        }
    }

    fun downloadTheme(req: HttpServerRequest, appType: String, filename: String) {
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/${appType}/themes/${filename}")
            if (!file.exists()) {
                sendFileNotExists(req)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
            }
        }
    }

    fun deleteTheme(req: HttpServerRequest, appType: String, filename: String) {
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/${appType}/themes/$filename")
            val trashes = File("users/${token.userId}/trashes/${appType}/themes")
            if (!trashes.exists()) {
                trashes.mkdirs()
            }
            if (file.exists()) {
                Files.move(
                    file.toPath(),
                    Paths.get("${trashes.path}/${filename}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                ServerDatabase.notifyFileDelete("${trashes.path}/${filename}")
                ServerDatabase.saveEvent(
                    token = token,
                    action = "delete_theme",
                    value = filename,
                    appType = appType
                )
                req.response().end(Messages.SUCCESS)
            } else {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            }
        }
    }

    fun getColors(req: HttpServerRequest, appType: String) {
        handleAuthorization(req) { token ->
            val file = File("users/${token.userId}/${appType}/colors")
            if (!file.exists()) {
                req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
            } else {
                req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
            }
        }
    }

    fun uploadColors(req: HttpServerRequest, appType: String) {
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val file = File("users/${token.userId}/${appType}/colors")
                file.writeText(buffer.toString())
                ServerDatabase.saveEvent(token = token, action = "upload_colors", value = "", appType = appType)

                sendSuccess(req)
            }
        }
    }

}