package com.amphi.handlers.music

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppAlbumsRequest {

    fun getAlbums(req: HttpServerRequest) {
        sendNotFound(req)
    }

}