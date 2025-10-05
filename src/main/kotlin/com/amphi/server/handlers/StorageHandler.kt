package com.amphi.server.handlers

import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import java.io.File

object StorageHandler {
    private fun getDirectorySize(): Long {
        val directory = File("users")
        if (!directory.exists()) return 0

        return directory.walkBottomUp()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    private fun getFileSize(path: String): Long {
        val file = File(path)
        if(!file.exists()) {
            return 0
        }
        return file.length()
    }

    fun getStorageInfo(req: HttpServerRequest) {

        val usedSpace = getFileSize("database.db") + getFileSize("settings.txt") + getFileSize("white-list.txt") + getFileSize("black-list.txt") + getDirectorySize()

        val jsonObject = JsonObject()

        val file = File("/")
        val totalBytes = file.totalSpace
        val usableBytes = file.usableSpace
        jsonObject.put("total", totalBytes)
        jsonObject.put("usable", usableBytes)
        jsonObject.put("used", usedSpace)

        val response = req.response()
        response.putHeader("content-type", "application/json; charset=UTF-8")
        response.end(jsonObject.encode())
    }
}