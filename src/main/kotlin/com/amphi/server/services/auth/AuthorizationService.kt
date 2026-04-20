package com.amphi.server.services.auth

import com.amphi.server.models.Token
import io.vertx.core.Future

interface AuthorizationService {
  fun authenticateByToken(token: String) : Future<Token>
  fun deleteObsoleteTokens() : Future<Unit>
  fun getTokens() : Future<Set<Token>>
}
