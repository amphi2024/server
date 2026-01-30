package com.amphi.server.services.user

import com.amphi.server.configs.ServerPostgresDatabase.connection
import io.vertx.core.json.JsonArray

class UserPostgresService : UserService {
  override fun getUserIds(): Set<String> {
      TODO("Not yet implemented")
  }

  override fun logout(token: String) {
    TODO("Not yet implemented")
  }

  override fun login(
    id: String,
    deviceName: String,
    password: String,
    onAuthenticated: (Boolean, String?, String?) -> Unit
  ) {
    TODO("Not yet implemented")
  }

  override fun generatedToken(): String {
    TODO("Not yet implemented")
  }

  override fun register(id: String, name: String, password: String, onFailed: () -> Unit, onSuccess: () -> Unit) {
    TODO("Not yet implemented")
  }

  override fun changePassword(
    oldPassword: String,
    password: String,
    id: String,
    onAuthenticationFailed: () -> Unit,
    onSuccess: () -> Unit
  ) {
    TODO("Not yet implemented")
  }

  override fun changeUsername(name: String, id: String) {
    TODO("Not yet implemented")
  }

}
