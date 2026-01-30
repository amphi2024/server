package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.common.handleAuthorization
import com.amphi.server.common.sendSuccess
import com.amphi.server.configs.AppConfig
import com.amphi.server.eventService
import io.vertx.core.http.HttpServerRequest
import java.io.File

object ThemeHandler {

    fun getColors(req: HttpServerRequest, appType: String) {
        handleAuthorization(req) { token ->
            val file = File(AppConfig.storage.data, "${token.userId}/${appType}/colors")
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
                val file = File(AppConfig.storage.data,"${token.userId}/${appType}/colors")
                file.writeText(buffer.toString())
                eventService.saveEvent(token = token, action = "upload_colors", value = "", appType = appType)

                sendSuccess(req)
            }
        }
    }

}