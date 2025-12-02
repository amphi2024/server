package com.amphi.server.utils

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.sql.ResultSet

fun ResultSet.getNullableString(columnLabel: String): String? {
    return getObject(columnLabel) as? String
}

fun ResultSet.getNullableLong(columnLabel: String): Long? {
    return getObject(columnLabel)?.let { (it as Number).toLong() }
}

fun ResultSet.getNullableInt(columnLabel: String): Int? {
    return getObject(columnLabel)?.let { (it as Number).toInt() }
}

fun ResultSet.getNullableJsonArray(columnLabel: String): JsonArray? {
    return getObject(columnLabel)?.let { value ->
        JsonArray(value as? String)
    }
}


fun ResultSet.getJsonArray(columnLabel: String): JsonArray {
    return getObject(columnLabel)?.let { value ->
        JsonArray(value as? String)
    } ?: JsonArray()
}

fun ResultSet.getJsonObject(columnLabel: String): JsonObject {
    return getObject(columnLabel)?.let { value ->
        JsonObject(value as? String)
    } ?: JsonObject()
}