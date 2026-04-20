package com.amphi.server.services.user

import io.vertx.core.Future

interface UserService {
  fun getUserIds(): Future<Set<String>>
  fun logout(token: String) : Future<Unit>
  fun login(id: String, deviceName: String, password: String) : Future<String>
  fun generatedToken() : Future<String>
  fun register(id: String, name: String, password: String) : Future<Unit>
  fun changePassword(oldPassword: String, password: String, id: String) : Future<Unit>
  fun changeUsername(name: String, id: String) : Future<Unit>
}