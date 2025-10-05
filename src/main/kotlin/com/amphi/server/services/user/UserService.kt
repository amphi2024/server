package com.amphi.server.services.user

import io.vertx.core.json.JsonArray

interface UserService {
  fun getUserIds(): JsonArray
  fun logout(token: String)
  fun login(id: String, deviceName: String, password: String, onAuthenticated: (Boolean, String?, String?) -> Unit)
  fun generatedToken() : String
  fun register(id: String, name: String, password: String, onFailed: () -> Unit, onSuccess: () -> Unit)
  fun changePassword(oldPassword: String, password: String, id: String, onAuthenticationFailed: () -> Unit, onSuccess: () -> Unit)
  fun changeUsername(name: String, id: String)
}
