package com.amphi.handlers.music

import com.amphi.sendNotFound
import io.vertx.core.http.HttpServerRequest

object MusicAppPlaylistsRequest {

    fun getPlaylists(req: HttpServerRequest) {
        sendNotFound(req)
    }

}