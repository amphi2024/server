package com.amphi.server.utils

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun JsonObject.getNullableJsonArray(key: String) : JsonArray? {
    return if(containsKey(key)) {
        getJsonArray(key)
    }
    else {
        null
    }
}

fun JsonObject.getNullableString(key: String) : String? {
    return if(containsKey(key)) {
        getString(key)
    }
    else {
        null
    }
}

fun JsonObject.getNullableLong(key: String) : Long? {
    return if(containsKey(key)) {
        getLong(key)
    }
    else {
        null
    }
}

fun JsonObject.getNullableInt(key: String) : Int? {
    return if(containsKey(key)) {
        getInteger(key)
    }
    else {
        null
    }
}