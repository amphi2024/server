package com.amphi.handlers.music

import com.amphi.*
import io.vertx.core.http.HttpServerRequest

object MusicAppArtistsRequest : MusicAppRequest {

    fun getArtists(req: HttpServerRequest) {
        sendNotFound(req)
    }

    fun getArtistInfo(req: HttpServerRequest, split: List<String>) {
        getInfo(req, split, "artists")
    }

    fun uploadArtistInfo(req: HttpServerRequest, split: List<String>) {
        uploadInfo(req, split, "artists", "upload_artist_album")
    }

    fun uploadArtistFile(req: HttpServerRequest, split: List<String>) {
        uploadFile(req, split, "artists", "upload_artist_file")
    }

    fun deleteArtist(req: HttpServerRequest, split: List<String>) {
        sendNotFound(req)
    }

}