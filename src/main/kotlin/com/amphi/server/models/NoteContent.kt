package com.amphi.server.models

data class NoteContent(
    val value: Any,
    val type: String = "text",
    val style: MutableMap<String, Any>? = null
)