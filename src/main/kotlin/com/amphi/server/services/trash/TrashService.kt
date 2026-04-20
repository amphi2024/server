package com.amphi.server.services.trash

import com.amphi.server.models.TrashLog
import io.vertx.core.Future

interface TrashService {
    fun notifyFileDelete(filePath: String) : Future<Unit>

    fun getTrashLogs(): Future<Set<TrashLog>>

    fun deleteTrashLog(path: String) : Future<Unit>
}
