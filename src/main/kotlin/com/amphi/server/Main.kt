package com.amphi.server

import com.amphi.server.common.Messages
import com.amphi.server.common.StatusCode
import com.amphi.server.configs.SQLITE
import com.amphi.server.configs.ServerSettings
import com.amphi.server.handlers.StorageHandler
import com.amphi.server.routes.MusicRouter
import com.amphi.server.routes.NotesRouter
import com.amphi.server.routes.UserRouter
import com.amphi.server.services.auth.AuthorizationPostgresService
import com.amphi.server.services.auth.AuthorizationSqliteService
import com.amphi.server.services.event.EventPostgresService
import com.amphi.server.services.event.EventSqliteService
import com.amphi.server.services.trash.TrashPostgresService
import com.amphi.server.services.trash.TrashSqliteService
import com.amphi.server.common.sendNotFound
import com.amphi.server.configs.ServerPostgresDatabase
import com.amphi.server.configs.ServerSqliteDatabase
import com.amphi.server.models.music.MusicDatabase
import com.amphi.server.models.notes.NotesDatabase
import com.amphi.server.routes.CloudRouter
import com.amphi.server.routes.PhotosRouter
import com.amphi.server.utils.RateLimiter
import com.amphi.server.utils.deleteObsoleteCloudFiles
import com.amphi.server.utils.deleteObsoleteFilesInTrash
import com.amphi.server.utils.deleteObsoleteAttachments
import com.amphi.server.utils.migration.migrateMusic
import com.amphi.server.utils.migration.migrateNotes
import com.amphi.server.utils.migration.migratePhotos
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val VERSION = "2.0.0"

val authorizationService = if (ServerSettings.databaseType == SQLITE) AuthorizationSqliteService() else AuthorizationPostgresService()
val eventService = if (ServerSettings.databaseType == SQLITE) EventSqliteService() else EventPostgresService()
val trashService = if (ServerSettings.databaseType == SQLITE) TrashSqliteService() else TrashPostgresService()

class App : AbstractVerticle(), Handler<HttpServerRequest> {

    override fun handle(req: HttpServerRequest) {
        try {
            val path = req.path()
            when {
                path == "/" -> {
                    req.response().putHeader("content-type", "text/plain").end("Server is running. Let's go!")
                }
                path == "/version" -> req.response().putHeader("content-type", "text/plain").end(VERSION)
                path == "/storage" -> StorageHandler.getStorageInfo(req)
                path.startsWith("/users") -> UserRouter.route(req)
                path.startsWith("/notes") -> NotesRouter.route(req)
                path.startsWith("/music") -> MusicRouter.route(req)
                path.startsWith("/photos") -> PhotosRouter.route(req)
                path.startsWith("/cloud") -> CloudRouter.route(req)
                else -> sendNotFound(req)
            }
        } catch (e: Exception) {
            println(e)
            req.response()
                .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR)
                .end(Messages.ERROR)
        }
    }

    override fun start() {

        vertx.createHttpServer().requestHandler{req ->

            val ipAddress = req.remoteAddress().hostAddress()
//            println("request - ip: $ipAddress, ${req.path()} ${req.method().name()}")
            if(RateLimiter.isAllowed(ipAddress)) {
                if(ServerSettings.whitelistOnly) {
                    if(ServerSettings.inWhiteList(ipAddress)) {
                        handle(req)
                    }
                    else {
                        req.response()
                            .setStatusCode(StatusCode.FORBIDDEN)
                            .putHeader("content-type", "text/plain; charset=UTF-8")
                            .end(Messages.WHITE_LIST_ONLY)
                    }
                }
                else if(ServerSettings.inBlackList(ipAddress)) {
                    req.response()
                        .setStatusCode(StatusCode.FORBIDDEN)
                        .putHeader("content-type", "text/plain; charset=UTF-8")
                        .end(ServerSettings.blockMessage)
                }
                else {
                    handle(req)
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

    println("Server is running. Let's go! (port: ${ServerSettings.port}, version: $VERSION)")
    Runtime.getRuntime().addShutdownHook(Thread {
        if(ServerSettings.databaseType == SQLITE) {
            ServerSqliteDatabase.close()
        }
        else {
            ServerPostgresDatabase.close()
        }
    })

    val scheduler = Executors.newScheduledThreadPool(1)

    val task = Runnable {
        try {

            authorizationService.syncTokensLastAccess()
            authorizationService.deleteObsoleteTokens()
            val users = File("users")
            val trashLogs = trashService.getTrashLogs()
            if (users.exists()) {
                users.listFiles()?.forEach { userDirectory ->
                    val oldTrash = File("${userDirectory.path}/trashes")

                    if(oldTrash.exists() && oldTrash.listFiles().isNullOrEmpty()) {
                        oldTrash.delete()
                    }

                    deleteObsoleteCloudFiles(userDirectory)

                    val userId = userDirectory.name

                    val trash = File("users/$userId/trash")
                    deleteObsoleteFilesInTrash(trash, trashLogs)

                    migrateNotes(userDirectory)
                    migrateMusic(userDirectory)
                    migratePhotos(userDirectory)

                    val notesDBFile = File("users/$userId/notes/notes.db")
                    if (notesDBFile.exists()) {
                        val database = NotesDatabase(userId)
                        database.deleteObsoleteNotes()
                        val notes = database.getNotes()
                        deleteObsoleteAttachments(notes, userId)
                        database.close()
                    }
                    
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS)

}