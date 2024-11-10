package com.amphi.models

import io.vertx.core.http.ServerWebSocket

data class ConnectedUser(val webSocket: ServerWebSocket, val token: Token)
