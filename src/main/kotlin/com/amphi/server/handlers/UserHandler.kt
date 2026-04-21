package com.amphi.server.handlers

import com.amphi.server.common.*
import com.amphi.server.configs.AppConfig
import com.amphi.server.eventService
import com.amphi.server.logger
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
                    logger?.warn(
                        "[SECURITY] Register Rejected (Missing Fields): IP=${
                            req.remoteAddress().hostAddress()
                        }}"
                    )
                    sendAuthFailed(req)
                } else {
                    if (AppConfig.security.allowUserRegistration) {
                        userService.register(id = id, name = name, password = password)
                            .onSuccess {
                                sendSuccess(req)
                            }
                            .onFailure {
                                req.response().setStatusCode(StatusCode.BAD_REQUEST).end(Messages.ID_ALREADY_TAKEN)
                            }
                    } else {
                        logger?.warn(
                            "[SECURITY] Register Attempt Blocked (Registration Disabled): IP=${
                                req.remoteAddress().hostAddress()
                            }}"
                        )
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
                    logger?.warn("[SECURITY] Login Rejected (Missing Fields): IP=${req.remoteAddress().hostAddress()}}")
                    sendAuthFailed(req)
                } else {
                    userService.login(
                        id = id,
                        deviceName = deviceName,
                        password = password
                    ).onSuccess { result ->
                        logger?.info(
                            "[AUTH] Login Success: ID=$id, IP=${
                                req.remoteAddress().hostAddress()
                            }, Device=$deviceName"
                        )
                        val response = req.response()
                        response.putHeader("content-type", "text/plain")
                        response.end(result)
                    }
                        .onFailure { throwable ->
                            logger?.warn(
                                "[SECURITY] Login Failed: ID=$id, IP=${req.remoteAddress().hostAddress()}",
                                throwable
                            )
                            sendAuthFailed(req)
                        }
                }
            }
        }
    }

    fun logout(req: HttpServerRequest) {
        req.withAuth { token ->
            userService.logout(token.token).onSuccess {
                sendSuccess(req)
            }
                .onFailure {
                    sendInternalServerError(req)
                }
        }
    }

    fun changeUsername(req: HttpServerRequest) {
        req.withAuth { token ->
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
                        ).compose {
                            eventService.saveEvent(
                                value = name,
                                action = "rename_user",
                                token = token,
                                appType = null
                            )
                        }
                            .onSuccess {
                                logger?.info("[USER] Username Changed: ID=${token.userId}, NewName=$name")
                                sendSuccess(req)
                            }
                            .onFailure { throwable ->
                                logger?.info(
                                    "[USER] Username Change Failed: ID=${token.userId}, NewName=$name",
                                    throwable
                                )
                                sendInternalServerError(req)
                            }
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

                if (id.isNullOrBlank() || password.isNullOrBlank() || oldPassword.isNullOrBlank()) {
                    logger?.warn(
                        "[SECURITY] Change Password Rejected (Missing Fields): IP=${
                            req.remoteAddress().hostAddress()
                        }"
                    )
                    sendAuthFailed(req)
                } else {
                    if (id == userId) {
                        userService.changePassword(
                            id = id,
                            password = password,
                            oldPassword = oldPassword
                        )
                            .onSuccess {
                                logger?.info("[AUTH] Password Changed: ID=$id")
                                sendSuccess(req)
                            }
                            .onFailure { throwable ->
                                logger?.warn(
                                    "[SECURITY] Change Password Failed: ID=$id, IP=${
                                        req.remoteAddress().hostAddress()
                                    }", throwable
                                )
                                sendAuthFailed(req)
                            }
                    } else {
                        logger?.error(
                            "[SECURITY] Unauthorized Change Password Attempt: TargetID=$id, ActorID=$userId, IP=${
                                req.remoteAddress().hostAddress()
                            }"
                        )
                        sendAuthFailed(req)
                    }
                }
            }
        }
    }

    fun getUserIds(req: HttpServerRequest) {
        logger?.info("[ADMIN] User List Accessed: IP=${req.remoteAddress().hostAddress()}")
        userService.getUserIds().onSuccess { ids ->
            req.response().putHeader("content-type", "application/json; charset=UTF-8").end(
                "[${
                    ids.joinToString {
                        "\"$it\""
                    }
                }]"
            )
        }
            .onFailure { throwable ->
                logger?.error("[USER] Could not retrieve ID list", throwable)
                sendInternalServerError(req)
            }

    }
}
