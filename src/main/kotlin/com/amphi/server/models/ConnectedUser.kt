package com.amphi.server.models

import io.vertx.core.http.ServerWebSocket

data class ConnectedUser(val webSocket: ServerWebSocket, val token: Token)
