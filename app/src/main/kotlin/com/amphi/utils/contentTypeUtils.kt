package com.amphi.utils

fun contentTypeByExtension(fileExtension: String) : String {
    val contentType = when(fileExtension) {
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "tiff", "tif" -> "image/tiff"
        "svg" -> "image/svg+xml"
        "ico" -> "image/vnd.microsoft.icon"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "jfif" -> "image/jpeg"
        "pjpeg" -> "image/pjpeg"
        "pjp" -> "image/jpeg"
        "avif" -> "image/avif"

        "raw" -> "image/x-raw"
        "dng" -> "image/x-adobe-dng"
        "cr2" -> "image/x-canon-cr2"
        "nef" -> "image/x-nikon-nef"
        "arw" -> "image/x-sony-arw"
        "rw2" -> "image/x-panasonic-rw2"
        "orf" -> "image/x-olympus-orf"
        "sr2" -> "image/x-sony-sr2"
        "raf" -> "image/x-fuji-raf"
        "pef" -> "image/x-pentax-pef"

        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "wmv" -> "video/x-ms-wmv"
        "mkv" -> "video/x-matroska"
        "flv" -> "video/x-flv"
        "webm" -> "video/webm"
        "mpeg", "mpg" -> "video/mpeg"
        "m4v" -> "video/x-m4v"
        "3gp" -> "video/3gpp"
        "3g2" -> "video/3gpp2"
        "f4v" -> "video/x-f4v"
        "swf" -> "application/x-shockwave-flash"
        "vob" -> "video/dvd"
        "ts" -> "video/mp2t"
        else -> "application/octet-stream"
    }
    return contentType
}