package com.amphi.server.services.trash

import com.amphi.server.models.TrashLog

class TrashPostgresService : TrashService {
    override fun notifyFileDelete(filePath: String) {
        TODO("Not yet implemented")
    }

    override fun getTrashLogs(): List<TrashLog> {
        TODO("Not yet implemented")
    }

    override fun deleteTrashLog(path: String) {
        TODO("Not yet implemented")
    }
}
