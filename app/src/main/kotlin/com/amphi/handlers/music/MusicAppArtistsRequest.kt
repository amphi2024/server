package com.amphi.handlers.music

import io.vertx.core.http.HttpServerRequest

object MusicAppArtistsRequest : MusicAppRequest {

    fun getArtists(req: HttpServerRequest) {
        getItems(req, "artists")
    }

    fun getArtistFiles(req: HttpServerRequest, split: List<String>) {
        getFilesOfSomething(req, split, "artists")
    }

    fun getArtistInfo(req: HttpServerRequest, split: List<String>) {
        getInfo(req, split, "artists")
    }

    fun uploadArtistInfo(req: HttpServerRequest, split: List<String>) {
        uploadInfo(req, split, "artists", "upload_artist_info")
    }

    fun uploadArtistFile(req: HttpServerRequest, split: List<String>) {
        uploadFile(req, split, "artists", "upload_artist_file")
    }

    fun downloadArtistFile(req: HttpServerRequest, split: List<String>) {
        downloadFile(req, split, "artists")
    }

    fun deleteArtist(req: HttpServerRequest, split: List<String>) {
        delete(req, split, "artists", "delete_artist")
    }

}