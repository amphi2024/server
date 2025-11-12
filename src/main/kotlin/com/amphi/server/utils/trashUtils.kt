package com.amphi.server.utils

import com.amphi.server.trashService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun moveToTrash(userId: String, path: String, filename: String) {
    val trashDir = File("users/${userId}/trash/${path}")
    if(!trashDir.exists()) {
        trashDir.mkdirs()
    }
    Files.move(
        Paths.get("users/${userId}/$path/$filename"),
        Paths.get("${trashDir.path}/$filename"),
        StandardCopyOption.REPLACE_EXISTING
    )
    trashService.notifyFileDelete("${trashDir.path}/$filename")
}