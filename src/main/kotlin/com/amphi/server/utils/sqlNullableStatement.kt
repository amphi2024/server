package com.amphi.server.utils

import java.sql.PreparedStatement

fun PreparedStatement.setNullable(index: Int, value: Any?, sqlType: Int) {
    if (value != null) {
        when (value) {
            is String -> setString(index, value)
            is Int -> setInt(index, value)
            is Long -> setLong(index, value)
            else -> throw IllegalArgumentException("Unsupported type")
        }
    } else {
        setNull(index, sqlType)
    }
}