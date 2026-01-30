package com.amphi.server.services.user

interface UserService {
  fun getUserIds(): Set<String>
  fun logout(token: String)
  fun login(id: String, deviceName: String, password: String, onAuthenticated: (Boolean, String?, String?) -> Unit)
  fun generatedToken() : String
  fun register(id: String, name: String, password: String, onFailed: () -> Unit, onSuccess: () -> Unit)
  fun changePassword(oldPassword: String, password: String, id: String, onAuthenticationFailed: () -> Unit, onSuccess: () -> Unit)
  fun changeUsername(name: String, id: String)
}
