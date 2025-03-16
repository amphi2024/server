package com.amphi.handlers.music

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppArtistsRequest {

    fun getArtists(req: HttpServerRequest) {
        sendNotFound(req)
    }

}