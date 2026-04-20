package com.amphi.server.services.user

import io.vertx.core.Future

class UserPostgresService : UserService {
  override fun getUserIds(): Future<Set<String>> {
    TODO("Not yet implemented")
  }

  override fun logout(token: String): Future<Unit> {
    TODO("Not yet implemented")
  }

  override fun login(
    id: String,
    deviceName: String,
    password: String
  ): Future<String> {
    TODO("Not yet implemented")
  }

  override fun generatedToken(): Future<String> {
    TODO("Not yet implemented")
  }

  override fun register(
    id: String,
    name: String,
    password: String
  ): Future<Unit> {
    TODO("Not yet implemented")
  }

  override fun changePassword(
    oldPassword: String,
    password: String,
    id: String
  ): Future<Unit> {
    TODO("Not yet implemented")
  }

  override fun changeUsername(name: String, id: String): Future<Unit> {
    TODO("Not yet implemented")
  }

}
