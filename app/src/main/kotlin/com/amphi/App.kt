package com.amphi

import com.amphi.handlers.StorageHandler
import com.amphi.handlers.UserHandler
import com.amphi.handlers.cloud.CloudAppRequestHandler
import com.amphi.handlers.notes.NotesAppRequestHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class App : AbstractVerticle(), Handler<HttpServerRequest> {

    override fun handle(req: HttpServerRequest) {
        try {
            val path = req.path()
            when {
                path == "/" -> {
                    req.response().putHeader("content-type", "text/plain").end("Server is running. Let's go!")
                }
                path == "/version" -> req.response().putHeader("content-type", "text/plain").end("1.0.0")
                path == "/storage" -> StorageHandler.handleStorageInfo(req)
                path.startsWith("/users") -> UserHandler.handleUserRequest(req)
                path.startsWith("/notes") -> NotesAppRequestHandler.handleRequest(req)
                path.startsWith("/cloud") -> CloudAppRequestHandler.handleRequest(req)
            }
        } catch (e: Exception) {
            req.response()
                .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR)
                .end(Messages.ERROR)
        }
    }

    override fun start() {
      //  val options = HttpServerOptions()
//        if(File(ServerSettings.keystorePath).exists() && ServerSettings.httpsOnly) {
//            try {
//                options.isSsl = true
//                options.setKeyCertOptions(
//                    PfxOptions().
//                    setPath(ServerSettings.keystorePath).
//                    setPassword(ServerSettings.keystorePassword)
//                )
//            } catch (e: Exception) {
//                println("Failed to set up HTTPS: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//        vertx.createHttpServer(options)

        vertx.createHttpServer().requestHandler{req ->

            val ipAddress = req.remoteAddress().hostAddress()
            println("request - ip: $ipAddress, ${req.path()} ${req.method().name()}")
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
                   // if(!ServerSettings.inWhiteList(ipAddress)) {
                     // ServerSettings.writeLog("Request from non-whitelisted IP: $ipAddress")
                   // }
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

    println("Server is running. Let's go! (port: ${ServerSettings.port}, version: 1.0.0)")
    Runtime.getRuntime().addShutdownHook(Thread {
        ServerDatabase.close()
    })

    val scheduler = Executors.newScheduledThreadPool(1)

    val task = Runnable {
        try {

            ServerDatabase.syncTokensLastAccess()
            ServerDatabase.deleteObsoleteTokens()
            //ServerSettings.saveLogs()
            ServerFileUtils.deleteObsoleteFiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS)

}