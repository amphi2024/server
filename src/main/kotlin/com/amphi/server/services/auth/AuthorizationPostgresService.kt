package com.amphi.server.services.auth

import com.amphi.server.models.Token

class AuthorizationPostgresService : AuthorizationService {
  override fun authenticateByToken(token: String, onAuthenticated: (Token) -> Unit, onFailed: () -> Unit) {
    TODO("Not yet implemented")
  }

  override fun deleteObsoleteTokens() {
    TODO("Not yet implemented")
  }

  override fun generatedToken(): String {
    TODO("Not yet implemented")
  }

    override fun syncTokensLastAccess() {
        TODO("Not yet implemented")
    }

    override fun getTokens(): List<Token> {
        TODO("Not yet implemented")
    }

    override fun addToken(token: Token) {
        TODO("Not yet implemented")
    }
}