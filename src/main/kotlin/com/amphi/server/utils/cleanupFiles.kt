package com.amphi.server.utils

import com.amphi.server.models.cloud.CloudDatabase
import com.amphi.server.models.notes.Note
import com.amphi.server.models.TrashLog
import com.amphi.server.trashService
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.LinkedHashMap
import kotlin.collections.forEach

fun deleteObsoleteFilesInTrash(trash: File, trashLogs: List<TrashLog>) {
    trash.listFiles()?.forEach { file ->
        if (file.isFile) {
            trashLogs.forEach { trashLog ->
                if (trashLog.path == file.path) {
                    val period = Duration.between(
                        trashLog.timeStamp.atZone(ZoneId.systemDefault()).toInstant(),
                        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
                    )
                    if (period.toDays() > 30) {
                        file.delete()
                        trashService.deleteTrashLog(trashLog.path)
                    }
                }
            }
        } else if (file.isDirectory) {
            deleteObsoleteFilesInTrash(file, trashLogs)
        }
    }
}

fun deleteObsoleteCloudFiles(userDirectory: File) {
    val cloudDBFile = File("users/${userDirectory.name}/cloud/cloud.db")
    if (cloudDBFile.exists()) {
        val database = CloudDatabase(userDirectory.name)
        database.permanentlyDeleteObsoleteFiles()
        database.close()
    }
}

fun deleteObsoleteAttachments(noteList: List<Note>, userId: String) {
    val directory = File("users/$userId/notes/attachments")
    val notes = mutableMapOf<String, Note>()
    noteList.forEach { note ->
        notes[note.id] = note
    }

    directory.listFiles()?.forEach { sub1 ->
        sub1.listFiles()?.forEach { sub2 ->
            val images = mutableSetOf<String>()
            val videos = mutableSetOf<String>()
            val audio = mutableSetOf<String>()
            sub2.listFiles()?.forEach { attachmentDirectory ->
                val noteId = attachmentDirectory.name

                val note = notes[noteId]

                if(note == null) {
                    moveToTrash(
                        userId = userId,
                        path = "notes/attachments/${sub1.name}/${sub2.name}",
                        filename = noteId
                    )
                }
                else {
                    note.content.list.forEach { item ->
                        if(item is LinkedHashMap<*, *>) {
                            val value = item["value"]
                            if(value is String) {
                                when(item["type"]) {
                                    "img" -> images.add(value)
                                    "video" -> videos.add(value)
                                    "audio" -> audio.add(value)
                                }
                            }
                        }
                    }

                    note.deleteObsoleteMediaFiles(images, File("${attachmentDirectory.path}/images"), userId)
                    note.deleteObsoleteMediaFiles(videos, File("${attachmentDirectory.path}/videos"), userId)
                    note.deleteObsoleteMediaFiles(audio, File("${attachmentDirectory.path}/audio"), userId)
                }
            }
        }
    }
}