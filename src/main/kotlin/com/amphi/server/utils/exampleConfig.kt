package com.amphi.server.utils

import java.io.File

fun generateExampleConfig() {
    val file = File("config.example.yaml")
    file.writeText("""
        port: 8000
        login-expiration-days: 30

        security:
          allow-user-registration: true
          proxy:
            enabled: false
            real-ip-header: "X-Forwarded-For"
            trusted-proxies: [ "127.0.0.1", "::1" ]

          rate-limit:
            enabled: false
            interval-minutes: 10
            max-requests: 45000
            use-real-ip: true

          access-control:
            allowed-hosts:
              enabled: false
              list: []
            blacklist:
              enabled: true
              list: []
              block-message: "Get out of my house."
            whitelist:
              enabled: false
              list: []

        media:
          multi-res-video: false
          generate-thumbnail: false

        storage:
          data: "./data"
          logs: "./logs"

        database:
          # Supported types: sqlite
          # Planned: postgresql, mysql
          type: "sqlite"
        #  host: "localhost"
        #  port: 5432
        #  username: "admin"
        #  password: "password"
        #  database-name: "your_db"
    """.trimIndent())
}