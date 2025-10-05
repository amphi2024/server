package com.amphi.server.routes

import com.amphi.server.handlers.EventHandler
import com.amphi.server.handlers.NotesHandler
import com.amphi.server.handlers.ThemeHandler
import com.amphi.server.handlers.WebsocketHandler
import com.amphi.server.common.sendBadRequest
import com.amphi.server.common.sendNotFound
import io.vertx.core.http.HttpServerRequest

object NotesRouter {
    private val websocketHandler = WebsocketHandler()

    fun route(req: HttpServerRequest) {

        val split = req.path().split("/")
        when (split.size) {
            5 -> {  //    ex: /notes/{note1}/images/image1.png
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[3]) {
                            "images" -> NotesHandler.downloadFile(req, split, "images")
                            "videos" -> NotesHandler.downloadFile(req, split, "videos")
                            "files" -> NotesHandler.downloadFile(req, split, "files")
                            "audio" -> NotesHandler.downloadFile(req, split, "audio")
                            else -> sendBadRequest(req)
                        }
                    }

                    "POST" -> {
                        when (split[3]) {
                            "images" -> NotesHandler.uploadFile(req, split, "images")
                            "videos" -> NotesHandler.uploadFile(req, split, "videos")
                            "files" -> NotesHandler.uploadFile(req, split, "files")
                            "audio" -> NotesHandler.downloadFile(req, split, "audio")
                            else -> sendBadRequest(req)
                        }
                    }

                    "DELETE" -> {
                        when (split[3]) {
                            "images" -> NotesHandler.deleteFile(req, split, "images")
                            "videos" -> NotesHandler.deleteFile(req, split, "videos")
                            "files" -> NotesHandler.deleteFile(req, split, "files")
                            "audio" -> NotesHandler.downloadFile(req, split, "audio")
                            else -> sendBadRequest(req)
                        }
                    }

                    else -> sendNotFound(req)
                }

            }

            4 -> {   //   ex :   /notes/{note1}/images  ,   /notes/themes/{theme1}
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when (split[3]) {
                            "images" -> NotesHandler.getFiles(req, split, "images")
                            "videos" -> NotesHandler.getFiles(req, split, "videos")
                            "files" -> NotesHandler.getFiles(req, split, "files")
                            "audio" -> NotesHandler.downloadFile(req, split, "audio")
                            else -> {
                                if (split[2] == "themes") {
                                    ThemeHandler.downloadTheme(req, "notes", split[3])
                                } else {
                                    sendNotFound(req)
                                }
                            }
                        }
                    }

                    "POST" -> {
                        if (split[2] == "themes") {
                            ThemeHandler.uploadTheme(req, "notes", split[3])
                        } else {
                            sendNotFound(req)
                        }
                    }

                    "DELETE" -> {
                        if (split[2] == "themes") {
                            ThemeHandler.deleteTheme(req, "notes", split[3])
                        } else {
                            sendNotFound(req)
                        }
                    }

                    else -> sendNotFound(req)
                }
            }

            3 -> {  //    ex: /notes/note1.note
                when (req.method().name().uppercase()) {
                    "GET" -> {
                        when (req.path()) {
                            "/notes/colors" -> ThemeHandler.getColors(req, "notes")
                            "/notes/events" -> EventHandler.getEvents(req, "notes")
                            "/notes/themes" -> ThemeHandler.getThemes(req, "notes")
                            "/notes/sync" -> websocketHandler.handleWebsocket(req)
                            else -> NotesHandler.downloadNote(req, split)
                        }
                    }

                    "POST" -> {
                        when (req.path()) {
                            "/notes/colors" -> ThemeHandler.uploadColors(req, "notes")
                            else -> NotesHandler.uploadNote(req, split)
                        }
                    }

                    "DELETE" -> {
                        if (req.path() == "/notes/events") {
                            EventHandler.acknowledgeEvent(req)
                        } else {
                            NotesHandler.deleteNote(req, split)
                        }
                    }

                    else -> sendNotFound(req)
                }
            }

            2 -> {   //   ex:    /notes
                when (req.method().name().uppercase()) {
                    "GET" -> NotesHandler.getNotes(req)
                    else -> sendNotFound(req)
                }
            }

            else -> {
                sendNotFound(req)
            }
        }
    }
}