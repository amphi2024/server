package com.amphi.server.services.trash

import com.amphi.server.models.TrashLog

interface TrashService {
    fun notifyFileDelete(filePath: String)

    fun getTrashLogs(): List<TrashLog>

    fun deleteTrashLog(path: String)
}
