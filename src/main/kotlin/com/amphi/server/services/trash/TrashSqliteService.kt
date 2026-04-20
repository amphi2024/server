package com.amphi.server.services.trash

import com.amphi.server.configs.ServerSqliteDatabase.pool
import com.amphi.server.models.TrashLog
import io.vertx.core.Future
import io.vertx.sqlclient.Tuple
import java.time.Instant

class TrashSqliteService : TrashService {

    override fun notifyFileDelete(filePath: String): Future<Unit> {
        val instantValue = Instant.now().toEpochMilli()
        return pool.preparedQuery("INSERT INTO trash (path, timestamp) VALUES (? , ?) ON CONFLICT(path) DO UPDATE SET timestamp = ?")
            .execute(Tuple.of(filePath, instantValue, instantValue))
            .mapEmpty()
    }

    override fun getTrashLogs(): Future<Set<TrashLog>> {
        return pool.preparedQuery("SELECT path, timestamp FROM trash")
            .execute()
            .map { rows ->
                rows.map {
                    TrashLog(
                        path = it.getString("path"),
                        timeStamp = Instant.ofEpochMilli(it.getLong("timestamp"))
                    )
                }.toSet()
            }
    }

    override fun deleteTrashLog(path: String): Future<Unit> {
        return pool.preparedQuery("DELETE FROM trash WHERE path = ?")
            .execute(Tuple.of(path)).mapEmpty()
    }
}
