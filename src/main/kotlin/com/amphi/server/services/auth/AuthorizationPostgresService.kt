package com.amphi.server.services.auth

import com.amphi.server.models.Token
import io.vertx.core.Future

class AuthorizationPostgresService : AuthorizationService {

  override fun authenticateByToken(token: String): Future<Token> {
    TODO("Not yet implemented")
  }

  override fun deleteObsoleteTokens(): Future<Unit> {
    TODO("Not yet implemented")
  }

  override fun getTokens(): Future<Set<Token>> {
    TODO("Not yet implemented")
  }
}