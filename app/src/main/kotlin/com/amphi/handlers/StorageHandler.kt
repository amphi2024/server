package com.amphi.handlers

import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import java.io.File

object StorageHandler {

//    private fun formatBytes(bytes: Long): String {
//        return when {
//            bytes >= 1_099_511_627_776L -> String.format("%.2f TB", bytes / 1_099_511_627_776.0)
//            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
//            bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
//            bytes >= 1_024 -> String.format("%.2f KB", bytes / 1_024.0)
//            else -> "$bytes bytes"
//        }
//    }

//    private fun getDiskSpace(): Pair<String, String> {
//        val file = File("/")
//        val totalBytes = file.totalSpace
//        val usableBytes = file.usableSpace
//        return Pair(formatBytes(totalBytes), formatBytes(usableBytes))
//    }

   private  fun getDirectorySize(): Long {
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

    fun handleStorageInfo(req: HttpServerRequest) {
  
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