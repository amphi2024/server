package com.amphi.models

import java.time.LocalDateTime

data class Token(val userId: String, val token: String, var lastAccessed: LocalDateTime, val deviceName : String)