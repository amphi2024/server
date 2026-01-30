package com.amphi.server.handlers

import com.amphi.server.common.*
import com.amphi.server.configs.AppConfig
import com.amphi.server.eventService
import com.amphi.server.userService
import io.vertx.core.http.HttpServerRequest

object UserHandler {

    fun register(req: HttpServerRequest) {
        req.bodyHandler { buffer ->
            val jsonBody = buffer.toJsonObject()
            if (jsonBody == null) {
                sendAuthFailed(req)
            } else {
                val id = jsonBody.getString("id")
                val name = jsonBody.getString("name")
                val password = jsonBody.getString("password")

                if (id.isNullOrBlank() || name.isNullOrBlank() || password.isNullOrBlank()) {
                    sendAuthFailed(req)
                } else {
                    if(AppConfig.security.allowUserRegistration) {
                        userService.register(id = id, name = name, password = password, onFailed = {
                            req.response().setStatusCode(StatusCode.BAD_REQUEST).end(Messages.ID_ALREADY_TAKEN)
                        }, onSuccess = {
                            sendSuccess(req)
                        })
                    }
                    else {
                        req.response().setStatusCode(StatusCode.FORBIDDEN).end(Messages.NOT_ALLOWED_REGISTER)
                    }
                }
            }
        }
    }

    fun login(req: HttpServerRequest) {
        req.bodyHandler { buffer ->
            val jsonBody = buffer.toJsonObject()
            if (jsonBody == null) {
                sendAuthFailed(req)
            } else {
                val id = jsonBody.getString("id")
                val password = jsonBody.getString("password")
                val deviceName = req.headers().get("Device-Name")


                if (id.isNullOrBlank() || deviceName.isNullOrBlank() || password.isNullOrBlank()) {
                    sendAuthFailed(req)
                } else {
                    userService.login(
                        id = id,
                        deviceName = deviceName,
                        password = password
                    ) { isAuthenticated, token, username ->
                        if (isAuthenticated) {
                            val response = req.response()
                            response.putHeader("content-type", "text/plain")
                            response.end("${token};${username}")
                        } else {
                            sendAuthFailed(req)
                        }
                    }
                }
            }
        }
    }

    fun logout(req: HttpServerRequest) {
        handleAuthorization(req) {token ->
            userService.logout(token.token)
            sendSuccess(req)
        }
    }

    fun changeUsername(req: HttpServerRequest) {
        handleAuthorization(req) { token ->
            req.bodyHandler { buffer ->
                val jsonBody = buffer.toJsonObject()
                if (jsonBody == null) {
                    sendAuthFailed(req)
                } else {
                    val name = jsonBody.getString("name")

                    if (name.isNullOrBlank()) {
                        sendAuthFailed(req)
                    } else {
                        userService.changeUsername(
                            id = token.userId,
                            name = name,
                        )
                        eventService.saveEvent(
                            value = name,
                            action = "rename_user",
                            token = token,
                            appType = null
                        )
                        sendSuccess(req)
                    }
                }
            }
        }
    }

    fun changePassword(req: HttpServerRequest, userId: String) {
        req.bodyHandler { buffer ->
            val jsonBody = buffer.toJsonObject()
            if (jsonBody == null) {
                sendAuthFailed(req)
            } else {
                val id = jsonBody.getString("id")
                val password = jsonBody.getString("password")
                val oldPassword = jsonBody.getString("old_password")

                if (id.isNullOrBlank() ||password.isNullOrBlank() ||oldPassword.isNullOrBlank()) {
                    sendAuthFailed(req)
                } else {
                    if(id == userId) {
                        userService.changePassword(
                            id = id,
                            password = password,
                            oldPassword = oldPassword,
                            onAuthenticationFailed = {
                                sendAuthFailed(req)
                            },
                            onSuccess = {
                                sendSuccess(req)
                            }
                        )
                    }
                }
            }
        }
    }

    fun getUserIds(req: HttpServerRequest) {
        req.response().putHeader("content-type", "application/json; charset=UTF-8").end("[${userService.getUserIds().joinToString {
            "\"$it\""
        }}]")
    }
}
