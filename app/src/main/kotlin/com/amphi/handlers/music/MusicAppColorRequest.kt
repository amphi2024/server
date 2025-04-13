package com.amphi.handlers.music

import com.amphi.Messages
import com.amphi.ServerDatabase
import com.amphi.sendAuthFailed
import com.amphi.sendSuccess
import io.vertx.core.http.HttpServerRequest
import java.io.File

object MusicAppColorRequest {

    fun getColors(req: HttpServerRequest) {
        val requestToken = req.headers()["Authorization"]
        if (requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        } else {
            ServerDatabase.authenticateByToken(
                token = requestToken,
                onFailed = {
                    sendAuthFailed(req)
                },
                onAuthenticated = { token ->
                    val file = File("users/${token.userId}/music/colors")
                    if (!file.exists()) {
                        req.response().setStatusCode(404).end(Messages.FILE_NOT_EXISTS)
                    } else {
                        req.response().putHeader("content-type", "application/json; charset=UTF-8").end(file.readText())
                    }
                }
            )

        }
    }

    fun uploadColors(req: HttpServerRequest) {
        val requestToken = req.headers()["Authorization"]
        if(requestToken.isNullOrBlank()) {
            sendAuthFailed(req)
        }
        else {
            req.bodyHandler { buffer->

                ServerDatabase.authenticateByToken(
                    token = requestToken,
                    onFailed = {
                        sendAuthFailed(req)
                    },
                    onAuthenticated = { token ->
                        val file = File("users/${token.userId}/music/colors")
                        file.writeText(buffer.toString())
                        ServerDatabase.saveEvent(token = token, action = "upload_colors", value = "", appType = "notes")

                        sendSuccess(req)
                    }
                )
            }
        }
    }

}