package com.amphi.handlers.notes

import com.amphi.*
import com.amphi.handlers.ServerEventHandler
import com.amphi.handlers.ThemeHandler
import io.vertx.core.http.HttpServerRequest

object NotesAppRequestHandler {

    fun handleRequest(req: HttpServerRequest) {

        val split = req.path().split("/")
            when (split.size) {
                5 -> {  //    ex: /notes/{note1}/images/image1.png
                    when(req.method().name().uppercase()) {
                        "GET" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.downloadFile(req, split, "images")
                                "videos" -> NotesAppFileRequest.downloadFile(req, split, "videos")
                                "files" -> NotesAppFileRequest.downloadFile(req, split, "files")
                                "audio" -> NotesAppFileRequest.downloadFile(req, split, "audio")
                                else -> sendBadRequest(req)
                            }
                        }
                        "POST" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.uploadFile(req, split, "images")
                                "videos" -> NotesAppFileRequest.uploadFile(req, split, "videos")
                                "files" -> NotesAppFileRequest.uploadFile(req, split, "files")
                                "audio" -> NotesAppFileRequest.downloadFile(req, split, "audio")
                                else -> sendBadRequest(req)
                            }
                        }
                        "DELETE" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.deleteFile(req, split, "images")
                                "videos" -> NotesAppFileRequest.deleteFile(req, split, "videos")
                                "files" -> NotesAppFileRequest.deleteFile(req, split, "files")
                                "audio" -> NotesAppFileRequest.downloadFile(req, split, "audio")
                                else -> sendBadRequest(req)
                            }
                        }
                        else -> sendNotFound(req)
                    }

                }
                4 -> {   //   ex :   /notes/{note1}/images  ,   /notes/themes/{theme1}
                    when(req.method().name().uppercase()) {
                        "GET" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.getFiles(req, split, "images")
                                "videos" -> NotesAppFileRequest.getFiles(req, split, "videos")
                                "files" -> NotesAppFileRequest.getFiles(req, split, "files")
                                "audio" -> NotesAppFileRequest.downloadFile(req, split, "audio")
                                else -> {
                                    if(split[2] == "themes") {
                                        ThemeHandler.downloadTheme(req, "notes",split[3])
                                    }
                                    else {
                                        sendNotFound(req)
                                    }
                                }
                            }
                        }
                        "POST" -> {
                            if(split[2] == "themes") {
                                ThemeHandler.uploadTheme(req, "notes",split[3])
                            }
                            else {
                                sendNotFound(req)
                            }
                        }
                        "DELETE" -> {
                            if(split[2] == "themes") {
                                ThemeHandler.deleteTheme(req, "notes", split[3])
                            }
                            else {
                                sendNotFound(req)
                            }
                        }
                        else -> sendNotFound(req)
                    }
                }
                3 -> {  //    ex: /notes/note1.note
                    when(req.method().name().uppercase()) {
                        "GET" -> {
                            when (req.path()) {
                                "/notes/colors" -> ThemeHandler.getColors(req, "notes")
                                "/notes/events" -> ServerEventHandler.handleGetEvents(req, "notes")
                                "/notes/themes" -> ThemeHandler.getThemes(req, "notes")
                                "/notes/sync" -> NotesWebSocketHandler.handleWebsocket(req)
                                else -> NotesAppNoteRequest.downloadNote(req, split)
                            }
                        }
                        "POST" -> {
                            when(req.path()) {
                                "/notes/colors" -> ThemeHandler.uploadColors(req, "notes")
                                else -> NotesAppNoteRequest.uploadNote(req, split)
                            }
                        }
                        "DELETE" -> {
                            if(req.path() == "/notes/events") {
                                ServerEventHandler.handleAcknowledgeEvent(req)
                            }
                            else {
                                NotesAppNoteRequest.deleteNote(req, split)
                            }
                        }
                        else -> sendNotFound(req)
                    }
                }
                2 -> {   //   ex:    /notes
                    when(req.method().name().uppercase()) {
                        "GET" -> NotesAppNoteRequest.getNotes(req)
                        else -> sendNotFound(req)
                    }
                }
                else -> {
                    sendNotFound(req)
                }
            }
    }
}