package com.amphi

import com.amphi.models.TrashLog
import java.io.File
import java.time.Duration
import java.time.LocalDateTime

object ServerFileUtils {

    fun deleteObsoleteFiles() {
        val users = File("users")
        val trashLogs = ServerDatabase.getTrashLogs()
        if(users.exists()) {
            users.listFiles()?.forEach { userDirectory ->
                val trashes = File("users/${userDirectory.name}/trashes")
                emptyTrash(trashes, trashLogs)
            }
        }
    }

    private fun emptyTrash(trashes : File, trashLogs : List<TrashLog>) {
        trashes.listFiles()?.forEach { file ->
            if(file.isFile) {
                trashLogs.forEach { trashLog ->
                    if(trashLog.path == file.path) {
                        val period = Duration.between(trashLog.date, LocalDateTime.now())
                        if (period.toDays() > 30) {
                            file.delete()
                            ServerDatabase.deleteTrashLog(trashLog.path)
                        }
                    }
                }
            }
            else if(file.isDirectory) {
                emptyTrash(file, trashLogs)
            }
        }
    }
}