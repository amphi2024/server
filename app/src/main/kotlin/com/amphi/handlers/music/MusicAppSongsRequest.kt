package com.amphi.handlers.music

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppSongsRequest {

    fun getSongs(req: HttpServerRequest) {
        sendNotFound(req)
    }

}