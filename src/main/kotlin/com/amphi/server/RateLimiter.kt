package com.amphi.server

import com.amphi.server.configs.ServerSettings
import java.util.concurrent.ConcurrentHashMap

object RateLimiter {
    private val requestCounts = ConcurrentHashMap<String, Int>()
    private val requestTimestamps = ConcurrentHashMap<String, Long>()

    fun isAllowed(ip: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val requestCount = requestCounts.getOrDefault(ip, 0)
        val lastRequestTime = requestTimestamps.getOrDefault(ip, 0)

        return if (currentTime - lastRequestTime > ServerSettings.rateLimitIntervalMinutes * 60000) {
            requestCounts[ip] = 1
            requestTimestamps[ip] = currentTime
            true
        } else {
            if (requestCount < ServerSettings.rateLimitMaxRequests) {
                requestCounts[ip] = requestCount + 1
                true
            } else {
                false
            }
        }
    }
}