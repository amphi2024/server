package com.amphi.server.utils

import com.amphi.server.models.CloudDatabase
import com.amphi.server.models.TrashLog
import com.amphi.server.trashService
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
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