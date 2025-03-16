package com.amphi.handlers.music

import com.amphi.ServerDatabase
import com.amphi.sendAuthFailed
import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File

object MusicAppSongsRequest {

    fun getSongs(req: HttpServerRequest) {
        sendNotFound(req)
//        val requestToken = req.headers()["Authorization"]
//        if(requestToken.isNullOrBlank()) {
//            sendAuthFailed(req)
//        }
//        else {
//            ServerDatabase.authenticateByToken(
//                token = requestToken,
//                onFailed = {
//                    sendAuthFailed(req)
//                },
//                onAuthenticated = { token ->
//                    val jsonArray = JsonArray()
//                    val directory = File("users/${token.userId}/notes/songs")
//                    if (!directory.exists()) {
//                        directory.mkdirs()
//                    }
//                    val files = directory.listFiles()
//                    if (files != null) {
//                        for (file in files) {
//                            val jsonObject = JsonObject()
//                            jsonObject.put("filename", file.name)
//                            jsonObject.put("modified", file.lastModified())  // ex: 2024;7;13;18;30;13
//                            jsonArray.add(jsonObject)
//                        }
//                    }
//                    req.response().putHeader("content-type", "application/json").end(jsonArray.encode())
//                }
//            )
//        }
    }

}