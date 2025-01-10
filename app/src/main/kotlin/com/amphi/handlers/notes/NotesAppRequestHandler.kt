package com.amphi.handlers.notes

import com.amphi.*
import com.amphi.handlers.ServerEventHandler
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
                                "audios" -> NotesAppFileRequest.downloadFile(req, split, "audios")
                                else -> sendBadRequest(req)
                            }
                        }
                        "POST" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.uploadFile(req, split, "images")
                                "videos" -> NotesAppFileRequest.uploadFile(req, split, "videos")
                                "files" -> NotesAppFileRequest.uploadFile(req, split, "files")
                                "audios" -> NotesAppFileRequest.downloadFile(req, split, "audios")
                                else -> sendBadRequest(req)
                            }
                        }
                        "DELETE" -> {
                            when (split[3]) {
                                "images" -> NotesAppFileRequest.deleteFile(req, split, "images")
                                "videos" -> NotesAppFileRequest.deleteFile(req, split, "videos")
                                "files" -> NotesAppFileRequest.deleteFile(req, split, "files")
                                "audios" -> NotesAppFileRequest.downloadFile(req, split, "audios")
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
                                "audios" -> NotesAppFileRequest.downloadFile(req, split, "audios")
                                else -> {
                                    if(split[2] == "themes") {
                                        NotesAppThemeRequest.downloadTheme(req, split)
                                    }
                                    else {
                                        sendNotFound(req)
                                    }
                                }
                            }
                        }
                        "POST" -> {
                            if(split[2] == "themes") {
                                NotesAppThemeRequest.uploadTheme(req, split)
                            }
                            else {
                                sendNotFound(req)
                            }
                        }
                        "DELETE" -> {
                            if(split[2] == "themes") {
                                NotesAppThemeRequest.deleteTheme(req, split)
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
                                "/notes/colors" -> NotesAppColorRequest.getColors(req)
                                "/notes/events" -> ServerEventHandler.handleGetEvents(req, "notes")
                                "/notes/themes" -> NotesAppThemeRequest.getThemes(req)
                                "/notes/sync" -> NotesWebSocketHandler.handleWebsocket(req)
                                else -> NotesAppNoteRequest.downloadNote(req, split)
                            }
                        }
                        "POST" -> {
                            when(req.path()) {
                                "/notes/colors" -> NotesAppColorRequest.uploadColors(req)
                                "/notes/events" -> ServerEventHandler.handleAcknowledgeEvent(req)
                                else -> NotesAppNoteRequest.uploadNote(req, split)
                            }
                        }
                        "DELETE" -> {
                            NotesAppNoteRequest.deleteNote(req, split)
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