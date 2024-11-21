package com.amphi

import com.amphi.handlers.*
import com.amphi.handlers.notes.NotesAppRequestHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.net.PfxOptions
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class App : AbstractVerticle(), Handler<HttpServerRequest> {

    override fun handle(req: HttpServerRequest) {
        try {
            val path = req.path()
            when {
                path == "/" -> {
                    req.response().putHeader("content-type", "text/plain").end("server is running")
                }
                path == "/storage" -> StorageHandler.handleStorageInfo(req)
                path.startsWith("/users") -> UserHandler.handleUserRequest(req)
                path.startsWith("/notes") -> NotesAppRequestHandler.handleRequest(req)
                path.startsWith("/drive") -> DriveAppRequestHandler.handleRequest(req)
            }
        } catch (e: Exception) {
            req.response()
                .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR)
                .end(Messages.ERROR)
        }
    }

    override fun start() {
        val options = HttpServerOptions()
        if(File(ServerSettings.keystorePath).exists() && ServerSettings.httpsOnly) {
            try {
                options.isSsl = true
                options.setKeyCertOptions(
                    PfxOptions().
                    setPath(ServerSettings.keystorePath).
                    setPassword(ServerSettings.keystorePassword)
                )
            } catch (e: Exception) {
                println("Failed to set up HTTPS: ${e.message}")
                e.printStackTrace()
            }
        }

        vertx.createHttpServer(options).requestHandler{req ->

            val ipAddress = req.remoteAddress().hostAddress()
            println("request - ip: $ipAddress, ${req.path()} ")
            if(RateLimiter.isAllowed(ipAddress)) {
                if(ServerSettings.whitelistOnly) {
                    if(ServerSettings.inWhiteList(ipAddress)) {
                        handle(req)
                    }
                    else {
                        req.response()
                            .setStatusCode(403)
                            .putHeader("content-type", "text/plain; charset=UTF-8")
                            .end(Messages.WHITE_LIST_ONLY)
                    }
                }
                else if(ServerSettings.inBlackList(ipAddress)) {
                    req.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "text/plain; charset=UTF-8")
                        .end(ServerSettings.blockMessage)
                }
                else {
                    handle(req)
                    if(!ServerSettings.inWhiteList(ipAddress)) {
                       // ServerSettings.writeLog("Request from non-whitelisted IP: $ipAddress")
                    }
                }
            }
            else {
                req.response().setStatusCode(429).end(Messages.TOO_MANY_REQUESTS)
            }

    }.listen(ServerSettings.port)
    }
}
fun main() {

    val vertx = Vertx.vertx()
    vertx.deployVerticle(App())

    println("Server is running on ${ServerSettings.port} here we go!")
    Runtime.getRuntime().addShutdownHook(Thread {
        ServerSettings.saveLogs()
        ServerDatabase.close()
    })

//    vertx.setPeriodic(500) {
//        readCommand()
//    }

    val scheduler = Executors.newScheduledThreadPool(1)

    val task = Runnable {
        try {

            ServerDatabase.syncTokensLastAccess()
            ServerDatabase.deleteObsoleteTokens()
            ServerSettings.saveLogs()
            ServerFileUtils.deleteObsoleteFiles()
            UpdateService.checkForUpdate(vertx) {
                vertx.close()

                val currentDir = System.getProperty("user.dir") // 현재 작업 디렉토리
                val updateJarPath = "$currentDir/update-service.jar" // 같은 디렉토리의 업데이트 서비스 JAR 파일 경로
                val command = listOf("java", "-jar", updateJarPath)

                // ProcessBuilder를 사용하여 업데이트 서비스 JAR 파일 실행
                val processBuilder = ProcessBuilder(command)
                processBuilder.directory(File(currentDir)) // 작업 디렉토리 설정
                processBuilder.redirectErrorStream(true)

                processBuilder.start()
                exitProcess(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS)
}



fun readCommand() {

    try {
        val command = readlnOrNull()
        if (command != null) {
            when {
                command == "version" -> {
                    println("1.0.0")
                }

                command.startsWith("block") -> {

                }
                command == "help settings" -> {
                    println("port: Port address on which the server is running.")
                    println("open-registration: When set to true, it allows new user registrations. When set to false, it prevents registrations except for already registered users, thereby reducing security risks.")
                }

                else -> {
                    println("version: Show the server version.")
                    println("set {OPTION}: Changes the server settings. (example: set open-registration:false)")
                    println("block {IP}: Blocks all requests coming from entered address. (example: block ?.?.?.?)")
                    println("help settings: Show descriptions for each item in Settings.txt file.")
                }
            }
        }
    } catch (e: Exception) {
      //  println(e)
    }

//    readCommand()

}


//                when (req.path()) {
//                    "/" -> {
//                        val response = req.response()
//                        response.putHeader("content-type", "text/plain")
//                        response.end("Hi")
//                    }
//
//                    "/register" -> UserHandler.handleRegister(req)
//                    "/login" -> UserHandler.handleLogin(req)
//                    "/logout" -> UserHandler.handleLogout(req)
//                    "/rename_user" -> UserHandler.handleRename(req)
//                    "/change_password" -> UserHandler.handleChangePassword(req)
//                    "/get_user_ids" -> UserHandler.handleGetUserIds(req)
//
//                    "/storage_info" -> StorageHandler.handleStorageInfo(req)
//
//                    "/listen_updates" -> WebSocketHandler.handleWebSocket(req)
//
//                    "/get_events" -> ServerEventHandler.handleGetEvents(req)
//                    "/acknowledge_event" -> ServerEventHandler.handleAcknowledgeEvent(req)
//
//                    "/upload_note_theme" -> FileRequestHandler.handleFileUpload(req, "notes/themes")
//                    "/upload_note" -> FileRequestHandler.handleFileUpload(req, "notes/notes")
//                    "/upload_note_colors" -> FileRequestHandler.handleSimpleFileUpload(req, "notes/colors.json")
//                    "/upload_note_image" -> FileRequestHandler.handleFileUpload(req, "notes/images")
//                    "/upload_note_video" -> FileRequestHandler.handleFileUpload(req, "notes/videos")
//                    "/upload_note_audio" -> FileRequestHandler.handleFileUpload(req, "notes/audios")
//                    "/upload_music_theme" -> FileRequestHandler.handleFileUpload(req, "music/themes")
//                    "/upload_song" -> FileRequestHandler.handleFileUpload(req, "music/songs")
//                    "/upload_music_video" -> FileRequestHandler.handleFileUpload(req, "music/videos")
//
//                    "/download_note_theme" -> FileRequestHandler.handleFileDownload(req, "notes/themes")
//                    "/download_note" -> FileRequestHandler.handleFileDownload(req, "notes/notes")
//                    "/download_note_colors" -> FileRequestHandler.handleSimpleFileDownload(req, "notes/colors.json")
//                    "/download_note_image" -> FileRequestHandler.handleFileDownload(req, "notes/images")
//                    "/download_note_video" -> FileRequestHandler.handleFileDownload(req, "notes/videos")
//                    "/download_note_audio" -> FileRequestHandler.handleFileDownload(req, "notes/audios")
//                    "/download_music_theme" -> FileRequestHandler.handleFileDownload(req, "music/themes")
//                    "/download_song" -> FileRequestHandler.handleFileDownload(req, "music/songs")
//                    "/download_music_video" -> FileRequestHandler.handleFileDownload(req, "music/videos")
//
//                    "/get_note_themes" -> FileRequestHandler.handleGetFile(req, "notes/themes")
//                    "/get_notes" -> FileRequestHandler.handleGetFile(req, "notes/notes")
//                    "/get_note_images" -> FileRequestHandler.handleGetFile(req, "notes/images")
//                    "/get_note_videos" -> FileRequestHandler.handleGetFile(req, "notes/videos")
//                    "/get_note_audios" -> FileRequestHandler.handleGetFile(req, "notes/audios")
//                    "/get_music_themes" -> FileRequestHandler.handleGetFile(req, "music/themes")
//                    "/get_songs" -> FileRequestHandler.handleGetFile(req, "music/songs")
//                    "/get_music_videos" -> FileRequestHandler.handleGetFile(req, "music/videos")
//
//                    "/delete_note_theme" -> FileRequestHandler.handleDeleteFile(req, "notes/themes")
//                    "/delete_note" -> FileRequestHandler.handleDeleteFile(req, "notes/notes")
//                    "/delete_note_image" -> FileRequestHandler.handleDeleteFile(req, "notes/images")
//                    "/delete_note_video" -> FileRequestHandler.handleDeleteFile(req, "notes/videos")
//                    "/delete_note_audio" -> FileRequestHandler.handleDeleteFile(req, "notes/audios")
//                    "/delete_music_theme" -> FileRequestHandler.handleDeleteFile(req, "music/themes")
//                    "/delete_song" -> FileRequestHandler.handleDeleteFile(req, "music/songs")
//                    "/delete_music_video" -> FileRequestHandler.handleDeleteFile(req, "music/videos")
//                    else -> {
//                        req.response()
//                            .setStatusCode(404)
//                            .end(Messages.NOT_FOUND)
//                    }
//                }