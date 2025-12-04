package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.common.sendFileNotExists
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.handleAuthorization
import com.amphi.server.eventService
import com.amphi.server.trashService
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ThemeHandler {

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
                eventService.saveEvent(token = token, action = "upload_colors", value = "", appType = appType)

                sendSuccess(req)
            }
        }
    }

}