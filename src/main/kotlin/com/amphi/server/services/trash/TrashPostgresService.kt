package com.amphi.server.services.trash

import com.amphi.server.models.TrashLog
import io.vertx.core.Future

class TrashPostgresService : TrashService {
    override fun notifyFileDelete(filePath: String): Future<Unit> {
        TODO("Not yet implemented")
    }

    override fun getTrashLogs(): Future<Set<TrashLog>> {
        TODO("Not yet implemented")
    }

    override fun deleteTrashLog(path: String): Future<Unit> {
        TODO("Not yet implemented")
    }
}
