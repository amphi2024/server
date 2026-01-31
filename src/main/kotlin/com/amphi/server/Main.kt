package com.amphi.server

import com.amphi.server.common.Messages
import com.amphi.server.common.StatusCode
import com.amphi.server.common.sendNotFound
import com.amphi.server.configs.*
import com.amphi.server.handlers.StorageHandler
import com.amphi.server.models.music.MusicDatabase
import com.amphi.server.models.notes.NotesDatabase
import com.amphi.server.models.photos.PhotosDatabase
import com.amphi.server.routes.*
import com.amphi.server.services.auth.AuthorizationPostgresService
import com.amphi.server.services.auth.AuthorizationSqliteService
import com.amphi.server.services.event.EventPostgresService
import com.amphi.server.services.event.EventSqliteService
import com.amphi.server.services.trash.TrashPostgresService
import com.amphi.server.services.trash.TrashSqliteService
import com.amphi.server.services.user.UserPostgresService
import com.amphi.server.services.user.UserSqliteService
import com.amphi.server.utils.RateLimiter
import com.amphi.server.utils.deleteObsoleteAttachments
import com.amphi.server.utils.deleteObsoleteCloudFiles
import com.amphi.server.utils.deleteObsoleteFilesInTrash
import com.amphi.server.utils.migration.migrateMusic
import com.amphi.server.utils.migration.migrateNotes
import com.amphi.server.utils.migration.migratePhotos
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.internal.logging.Logger
import io.vertx.core.internal.logging.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val VERSION = "3.0.1"

val authorizationService =
    if (AppConfig.database.type == SQLITE) AuthorizationSqliteService() else AuthorizationPostgresService()
val eventService = if (AppConfig.database.type == SQLITE) EventSqliteService() else EventPostgresService()
val trashService = if (AppConfig.database.type == SQLITE) TrashSqliteService() else TrashPostgresService()
val userService = if(AppConfig.database.type == SQLITE) UserSqliteService() else UserPostgresService()
val logger: Logger? = LoggerFactory.getLogger(App::class.java)

class App : AbstractVerticle(), Handler<HttpServerRequest> {

    override fun handle(req: HttpServerRequest) {
        val path = req.path()
        if(RateLimiter.isAllowed(req.remoteAddress().hostAddress())) {
            try {
                logger?.info("[ACCESS] IP=${req.remoteAddress().hostAddress()}, Path=$path")
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
                    else -> {
                        logger?.info("[NOT-FOUND] IP=${req.remoteAddress().hostAddress()}, Path=$path")
                        sendNotFound(req)
                    }
                }
            } catch (e: Exception) {
                logger?.error("[INTERNAL-ERROR] IP=${req.remoteAddress().hostAddress()}, Path=$path", e)
                req.response()
                    .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR)
                    .end(Messages.ERROR)
            }
        }
        else {
            logger?.warn("[RATE-LIMIT] IP=${req.remoteAddress().hostAddress()}, Path=$path")
            req.response().setStatusCode(429).end(Messages.TOO_MANY_REQUESTS)
        }
    }

    private fun getClientIp(req: HttpServerRequest) : String? {
        if(AppConfig.security.proxy.enabled) {
            return req.headers().get(AppConfig.security.proxy.realIpHeader).split(",").firstOrNull()?.trim()
        }
        return req.remoteAddress().hostAddress()
    }

    override fun start() {
        vertx.createHttpServer().requestHandler { req ->
            val ipAddress = getClientIp(req)
            val path = req.path()
            if(ipAddress == null || (AppConfig.security.accessControl.allowedHosts.enabled && !AppConfig.security.accessControl.allowedHosts.list.contains(ipAddress))) {
                logger?.warn("[SECURITY] Invalid Host: IP=$ipAddress, Path=$path")
                req.response()
                    .setStatusCode(StatusCode.FORBIDDEN)
                    .putHeader("content-type", "text/plain; charset=UTF-8")
                    .end(Messages.FORBIDDEN_HOST)
                return@requestHandler
            }
            if(AppConfig.security.accessControl.whitelist.enabled) {
                if(AppConfig.security.accessControl.whitelist.list.contains(ipAddress)) {
                    handle(req)
                }
                else {
                    logger?.warn("[SECURITY] Whitelist Blocked: IP=$ipAddress, Path=$path")
                    req.response()
                        .setStatusCode(StatusCode.FORBIDDEN)
                        .putHeader("content-type", "text/plain; charset=UTF-8")
                        .end(Messages.WHITE_LIST_ONLY)
                }
                return@requestHandler
            }
            if(AppConfig.security.accessControl.blacklist.enabled && AppConfig.security.accessControl.blacklist.list.contains(ipAddress)) {
                logger?.warn("[SECURITY] Blacklist Blocked: IP=$ipAddress, Path=$path")
                req.response()
                    .setStatusCode(StatusCode.FORBIDDEN)
                    .putHeader("content-type", "text/plain; charset=UTF-8")
                    .end(AppConfig.security.accessControl.blacklist.blockMessage)
                return@requestHandler
            }
            handle(req)
        }.listen(AppConfig.port)
    }
}

fun main() {
    LogConfig.setup()

    val vertx = Vertx.vertx()
    vertx.deployVerticle(App())

    println("Server is running. Let's go! (port: ${AppConfig.port}, version: $VERSION)")
    Runtime.getRuntime().addShutdownHook(Thread {
        if (AppConfig.database.type == SQLITE) {
            ServerSqliteDatabase.close()
        } else {
            ServerPostgresDatabase.close()
        }
    })

    val scheduler = Executors.newScheduledThreadPool(1)

    val task = Runnable {
        try {

            authorizationService.syncTokensLastAccess()
            authorizationService.deleteObsoleteTokens()
            val data = File(AppConfig.storage.data)
            val trashLogs = trashService.getTrashLogs()
            if(!data.exists()) {
                data.mkdirs()
            }
            userService.getUserIds().forEach { userId ->
                val userDirectory = File(data, userId)
                if(!userDirectory.exists()) {
                    userDirectory.mkdirs()
                }
                val oldTrash = File(userDirectory, "trashes")

                if (oldTrash.exists() && oldTrash.listFiles().isNullOrEmpty()) {
                    oldTrash.delete()
                }

                val trash = File(userDirectory, "trash")
                deleteObsoleteFilesInTrash(trash, trashLogs)

                migrateNotes(userId, userDirectory)
                migrateMusic(userId, userDirectory)
                migratePhotos(userId, userDirectory)

                File(userDirectory, "notes").let {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                    val database = NotesDatabase(userId)
                    database.deleteObsoleteNotes()
                    val notes = database.getNotes()
                    deleteObsoleteAttachments(notes, userId)
                    database.close()
                }

                File(userDirectory, "music").let {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                    val database = MusicDatabase(userId)
                    database.deleteObsoleteItems()
                    database.close()
                }

                File(userDirectory, "photos").let {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                    val database = PhotosDatabase(userId)
                    database.deleteObsoleteItems()
                    database.close()
                }

                File(userDirectory, "cloud").let {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                    deleteObsoleteCloudFiles(userId)
                }
            }
        } catch (e: Exception) {
            logger?.error("[ERROR]", e)
        }
    }

    scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS)

}