package com.amphi.server.utils.migration

import com.amphi.server.configs.AppConfig
import com.amphi.server.trashService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun moveOldNoteFileToTrash(file: File, userId: String) {
    val targetDirectory = File(AppConfig.storage.data, "${userId}/trash/notes/notes")
    if(!targetDirectory.exists()) {
        targetDirectory.mkdirs()
    }

    Files.move(
        file.toPath(),
        Paths.get("${targetDirectory.path}/${file.name}"),
        StandardCopyOption.REPLACE_EXISTING
    )
    trashService.notifyFileDelete("${targetDirectory.path}/${file.name}")
}

fun moveOldAttachments(file: File, userId: String, newId: String) {
    val oldAttachments = File(AppConfig.storage.data,"${userId}/notes/notes/${file.nameWithoutExtension}")

    val targetDirectory = File(AppConfig.storage.data,"${userId}/notes/attachments/${newId[0]}/${newId[1]}")

    if(!targetDirectory.exists()) {
        targetDirectory.mkdirs()
    }

    if (oldAttachments.exists()) {
        Files.move(
            oldAttachments.toPath(),
            Paths.get("${targetDirectory.path}/$newId"),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}