package com.amphi.server.handlers

import com.amphi.server.common.Messages
import com.amphi.server.ServerDatabase
import com.amphi.server.configs.ServerSettings
import com.amphi.server.common.StatusCode
import com.amphi.server.common.sendAuthFailed
import com.amphi.server.common.sendSuccess
import com.amphi.server.common.handleAuthorization
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
                    if(ServerSettings.openRegistration) {
                        ServerDatabase.registerUser(id = id, name = name, password = password, onFailed = {
                            val response = req.response()
                            response.setStatusCode(StatusCode.BAD_REQUEST)
                            response.end(Messages.ID_ALREADY_TAKEN)
                        })

                        val response = req.response()
                        response.putHeader("content-type", "text/plain")
                        response.end(Messages.SUCCESS)
                    }
                    else {
                        val response = req.response()
                        response.setStatusCode(StatusCode.FORBIDDEN)
                        response.end(Messages.NOT_ALLOWED_REGISTER)
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
                    ServerDatabase.login(
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
        val requestToken = req.headers().get("Authorization")
            if (requestToken == null) {
                sendAuthFailed(req)
            } else {
                ServerDatabase.logout(requestToken)
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
                        ServerDatabase.renameUser(
                            id = token.userId,
                            name = name,
                        )
                        ServerDatabase.saveEvent(
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
                        ServerDatabase.changeUserPassword(
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
        req.response().putHeader("content-type", "application/json; charset=UTF-8").end(ServerDatabase.getUserIds().encode())
    }
}
