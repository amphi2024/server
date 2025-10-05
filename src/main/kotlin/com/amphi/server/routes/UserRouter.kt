package com.amphi.server.routes

import com.amphi.server.handlers.UserHandler
import io.vertx.core.http.HttpServerRequest

object UserRouter {
    fun route(req: HttpServerRequest) {
        when (req.method().name().uppercase()) {
            "GET" -> {
                when (req.path()) {
                    "/users" -> UserHandler.getUserIds(req)
                    "/users/logout" -> UserHandler.logout(req)
                }
            }
            "POST" -> {
                when (req.path()) {
                    "/users" -> UserHandler.register(req)
                    "/users/login" -> UserHandler.login(req)
                }
            }
            "PATCH" -> {
                val split = req.path().split("/")
                val userId = split[2]
                if (split.size > 3 && split[3] == "password") { //   users/{userId}/password
                    UserHandler.changePassword(req, userId)
                } else if(req.path() == "/users/name") {  //  users/{userId}
                    UserHandler.changeUsername(req)
                }
            }
        }
    }
}