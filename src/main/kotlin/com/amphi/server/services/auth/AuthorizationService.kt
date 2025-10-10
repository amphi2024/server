package com.amphi.server.services.auth

import com.amphi.server.models.Token

interface AuthorizationService {
  fun authenticateByToken(token: String, onAuthenticated: (Token) -> Unit, onFailed: () -> Unit)
  fun deleteObsoleteTokens()
  fun generatedToken(): String
  fun syncTokensLastAccess()
    fun getTokens(): List<Token>
    fun addToken(token: Token)
}
