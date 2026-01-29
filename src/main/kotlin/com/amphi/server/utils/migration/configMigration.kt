package com.amphi.server.utils.migration

import java.io.File

fun migrateConfig() {
    var port = 8000
    var openRegistration = true
    var loginExpirationPeriod = 30
    var blockMessage = "you are blocked"
    var whitelistOnly = false
    var rateLimitIntervalMinutes = 10
    var rateLimitMaxRequests = 45000
    var multiResVideo = false
    var generateMediaThumbnail = true

    val whitelist = mutableListOf<String>()
    val blacklist = mutableListOf<String>()
    
    val file = File("settings.txt")
    file.forEachLine { line ->
        val first = line.split(":")[0]
        val second = line.split(":")[1]
        when(first) {
            "port" -> {
                port = Integer.parseInt(second)
            }
            "open-registration" -> {
                openRegistration = second.toBoolean()
            }
            "login-expiration-period" -> {
                loginExpirationPeriod = Integer.parseInt(second)
            }
            "block-message" -> {
                blockMessage = second
            }
            "whitelist-only" -> {
                whitelistOnly = second.toBoolean()
            }
            "rate-limit-interval-minutes" -> {
                rateLimitIntervalMinutes = Integer.parseInt(second)
            }
            "rate-limit-max-requests" -> {
                rateLimitMaxRequests = Integer.parseInt(second)
            }
            "multi-res-video" -> {
                multiResVideo = second.toBoolean()
            }
            "generate-media-thumbnail" -> {
                generateMediaThumbnail = second.toBoolean()
            }
        }
    }
    val whiteListFile = File("white-list.txt")
    if(whiteListFile.exists()) {
        whiteListFile.forEachLine { line ->
            if(!line.contains("?")) {
                whitelist.add(line)
            }
        }
    }
    val blackListFile = File("black-list.txt")
    if(blackListFile.exists()) {
        blackListFile.forEachLine { line ->
            if(!line.contains("?")) {
                blacklist.add(line)
            }
        }
    }

    val configFile = File("config.yaml")
    configFile.writeText("""
        port: $port
        login-expiration-days: $loginExpirationPeriod

        security:
          allow-user-registration: $openRegistration
          proxy:
            enabled: false
            real-ip-header: "X-Forwarded-For"
            trusted-proxies: [ "127.0.0.1", "::1" ]

          rate-limit:
            enabled: true
            interval-minutes: $rateLimitIntervalMinutes
            max-requests: $rateLimitMaxRequests
            use-real-ip: true

          access-control:
            allowed-hosts:
              enabled: false
              list: []
            black-list:
              enabled: true
              list: [${blacklist.joinToString { text -> 
                  "'${text}'"
                    }}]
              block-message: "$blockMessage"
            white-list:
              enabled: $whitelistOnly
              list: [${whitelist.joinToString { text ->
                  "'${text}'"
                    }}]
        media:
          multi-res-video: $multiResVideo
          generate-thumbnail: $generateMediaThumbnail

        storage:
          data: "./users"
          logs: "./logs"

        database:
          type: "sqlite"
        #  host: "localhost"
        #  port: 5432
        #  username: "admin"
        #  password: "password"
        #  database-name: "your_db"
    """.trimIndent())
}