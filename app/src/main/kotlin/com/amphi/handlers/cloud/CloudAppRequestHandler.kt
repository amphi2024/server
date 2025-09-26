package com.amphi.handlers.cloud

import com.amphi.handlers.ServerEventHandler
import com.amphi.handlers.ThemeHandler
import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object CloudAppRequestHandler {
    fun handleRequest(req: HttpServerRequest) {
        val split = req.path().split("/")
        when (split.size) {
            5 -> { // /cloud/files/{my-file}/upload
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when(split[4]) {
                            "download" -> downloadFile(req, split)
                            else -> sendNotFound(req)
                        }

                    }

                    "POST" -> {
                        when(split[4]) {
                            "upload" -> uploadFile(req, split)
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
                            "files" -> downloadFileInfo(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    "PATCH" -> {
                        when(split[2]) {
                            "files" -> updateFileInfo(req, split)
                            else -> sendNotFound(req)
                        }
                    }

                    "DELETE" -> {
                        when(split[2]) {
                            "files" -> deleteFile(req, split)
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
                            getFiles(req)
                        } else {
                            when (req.path()) {
                                "/cloud/colors" -> ThemeHandler.getColors(req, "cloud")
                                "/cloud/events" -> ServerEventHandler.handleGetEvents(req, "cloud")
                                "/cloud/themes" -> ThemeHandler.getThemes(req, "cloud")
                                "/cloud/sync" -> CloudWebSocketHandler.handleWebsocket(req)
                                else -> sendNotFound(req)
                            }
                        }
                    }

                    "POST" -> {
                        when (req.path()) {
                            "/cloud/files" -> createFile(req)
                            else -> sendNotFound(req)
                        }
                    }

                    "DELETE" -> {
                        if (req.path() == "/cloud/events") {
                            ServerEventHandler.handleAcknowledgeEvent(req)
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