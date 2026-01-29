package com.amphi.server.utils

import com.amphi.server.configs.AppConfig
import java.util.concurrent.ConcurrentHashMap

object RateLimiter {
    private val requestCounts = ConcurrentHashMap<String, Int>()
    private val requestTimestamps = ConcurrentHashMap<String, Long>()

    fun isAllowed(ip: String): Boolean {
        if(!AppConfig.security.rateLimit.enabled) {
            return true
        }
        val currentTime = System.currentTimeMillis()
        val requestCount = requestCounts.getOrDefault(ip, 0)
        val lastRequestTime = requestTimestamps.getOrDefault(ip, 0)

        return if (currentTime - lastRequestTime > AppConfig.security.rateLimit.intervalMinutes * 60000) {
            requestCounts[ip] = 1
            requestTimestamps[ip] = currentTime
            true
        } else {
            if (requestCount < AppConfig.security.rateLimit.maxRequests) {
                requestCounts[ip] = requestCount + 1
                true
            } else {
                false
            }
        }
    }
}