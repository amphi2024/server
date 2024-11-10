package com.amphi.handlers

import com.amphi.*
import io.vertx.core.http.HttpServerRequest

object UserHandler {

    fun handleUserRequest(req: HttpServerRequest) {
        when (req.method().name().uppercase()) {
            "GET" -> {
                when (req.path()) {
                    "/users" -> handleGetUserIds(req)
                    "/users/logout" -> handleLogout(req)
                }
            }
            "POST" -> {
                when (req.path()) {
                    "/users" -> handleRegister(req)
                    "/users/login" -> handleLogin(req)
                }
            }
            "PATCH" -> {
                val split = req.path().split("/")
                val userId = split[2]
                if (split.size > 3 && split[3] == "password") { //   users/{userId}/password
                    handleChangePassword(req, userId)
                } else if(req.path() == "/users/name") {  //  users/{userId}
                    handleRename(req)
                }
            }
        }
    }

    private fun handleRegister(req: HttpServerRequest) {
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

    private fun handleLogin(req: HttpServerRequest) {
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

    private fun handleLogout(req: HttpServerRequest) {
        val requestToken = req.headers().get("Authorization")
            if (requestToken == null) {
              sendAuthFailed(req)
            } else {
                ServerDatabase.logout(requestToken)
                sendSuccess(req)
            }

    }

    private fun handleRename(req: HttpServerRequest) {
        val requestToken = req.headers().get("Authorization")
        req.bodyHandler { buffer ->
            val jsonBody = buffer.toJsonObject()
            if (jsonBody == null) {
                 sendAuthFailed(req)
            } else {
                val name = jsonBody.getString("name")

                if (name.isNullOrBlank() || requestToken.isNullOrBlank()) {
                     sendAuthFailed(req)
                } else {
                    ServerDatabase.authenticateByToken(token = requestToken,
                        onFailed = {
                             sendAuthFailed(req)
                        },
                        onAuthenticated = {token ->
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
                    })
                    sendSuccess(req)
                }
            }
        }
    }

    private fun handleChangePassword(req: HttpServerRequest, userId: String) {
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

    private fun handleGetUserIds(req: HttpServerRequest) {
        req.response().putHeader("content-type", "application/json").end(ServerDatabase.getUserIds().encode())
    }
}
