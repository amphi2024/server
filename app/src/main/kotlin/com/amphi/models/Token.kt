package com.amphi.models

import java.time.Instant

data class Token(val userId: String, val token: String, var lastAccessed: Instant, val deviceName : String)