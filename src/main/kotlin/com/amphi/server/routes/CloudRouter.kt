package com.amphi.server.routes

import com.amphi.server.common.sendNotFound
import com.amphi.server.handlers.CloudHandler
import com.amphi.server.handlers.EventHandler
import com.amphi.server.handlers.ThemeHandler
import com.amphi.server.handlers.WebsocketHandler
import io.vertx.core.http.HttpServerRequest

object CloudRouter {

    private val websocketHandler = WebsocketHandler()

    fun route(req: HttpServerRequest) {
        val split = req.path().split("/")
        when (split.size) {
            5 -> { // /cloud/files/{my-file}/upload
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when(split[4]) {
                            "download" -> CloudHandler.downloadFile(req, split)
                            else -> sendNotFound(req)
                        }

                    }

                    "POST" -> {
                        when(split[4]) {
                            "upload" -> CloudHandler.uploadFile(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    else -> sendNotFound(req)
                }
            }

            4-> {
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when(split[2]) {
                            "files" -> CloudHandler.downloadFileInfo(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    "PATCH" -> {
                        when(split[2]) {
                            "files" -> CloudHandler.updateFileInfo(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    "DELETE" -> {
                        when(split[2]) {
                            "files" -> CloudHandler.deleteFile(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    else -> sendNotFound(req)
                }
            }

            3 -> {
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        if (req.path().startsWith("/cloud/files")) {
                            CloudHandler.getFiles(req)
                        } else {
                            when (req.path()) {
                                "/cloud/colors" -> ThemeHandler.getColors(req, "cloud")
                                "/cloud/events" -> EventHandler.getEvents(req, "cloud")
                                "/cloud/themes" -> ThemeHandler.getThemes(req, "cloud")
                                "/cloud/sync" -> websocketHandler.handleWebsocket(req)
                                else -> sendNotFound(req)
                            }
                        }
                    }

                    "POST" -> {
                        when (req.path()) {
                            "/cloud/files" -> CloudHandler.createFile(req)
                            else -> sendNotFound(req)
                        }
                    }

                    "DELETE" -> {
                        if (req.path() == "/cloud/events") {
                            EventHandler.acknowledgeEvent(req)
                        } else {
                            sendNotFound(req)
                        }
                    }

                    else -> sendNotFound(req)
                }
            }

            else -> sendNotFound(req)
        }
    }
}