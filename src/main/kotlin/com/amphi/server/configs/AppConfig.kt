package com.amphi.server.configs

import com.amphi.server.utils.migration.migrateConfig
import net.mamoe.yamlkt.Yaml
import java.io.File
import kotlin.system.exitProcess

object AppConfig {

    val port: Int
    val loginExpirationDays: Int
    val security: SecurityConfig
    val media: MediaConfig
    val storage: StorageConfig
    val database: DatabaseConfig

    init {
        @Suppress("UNCHECKED_CAST")
        try {
            val file = File("config.yaml")
            if(!file.exists() && File("settings.txt").exists()) {
                migrateConfig()
            }
            val data = Yaml.decodeMapFromString(file.readText())
            port = data.getValue("port") as Int
            loginExpirationDays = data.getValue("login-expiration-days") as Int
            (data.getValue("security") as Map<*, *>).let {
                security = SecurityConfig(
                    allowUserRegistration = it["allow-user-registration"] as Boolean,
                    proxy = (it["proxy"] as Map<*, *>).let { config ->
                        ProxyConfig(
                            enabled = config["enabled"] as Boolean,
                            realIpHeader = config["real-ip-header"] as String,
                            trustedProxies = config["trusted-proxies"] as List<String>
                        )
                    },
                    rateLimit = (it["rate-limit"] as Map<*, *>).let { config ->
                        RateLimitConfig(
                            enabled = config["enabled"] as Boolean,
                            intervalMinutes = config["interval-minutes"] as Int,
                            maxRequests = config["max-requests"] as Int,
                            useRealIp = config["use-real-ip"] as Boolean
                        )
                    },
                    accessControl = (it["access-control"] as Map<*, *>).let { config ->
                        AccessControlConfig(
                            allowedHosts = (config["allowed-hosts"] as Map<*, *>).let { hosts ->
                                val list = hosts["list"] as List<*>
                                AllowedHostsConfig(
                                    enabled = hosts["enabled"] as Boolean,
                                    list = list as List<String>
                                )
                            },
                            blackList = (config["black-list"] as Map<*, *>).let { black ->
                                val list = black["list"] as List<*>
                                BlackListConfig(
                                    enabled = black["enabled"] as Boolean,
                                    list = list as List<String>,
                                    blockMessage = black["block-message"] as String
                                )
                            },
                            whiteList = (config["white-list"] as Map<*, *>).let { white ->
                                val list = white["list"] as List<*>
                                WhiteListConfig(
                                    enabled = white["enabled"] as Boolean,
                                    list = list as List<String>
                                )
                            }
                        )
                    }
                )
            }
            (data.getValue("media") as Map<*, *>).let {
                media = MediaConfig(
                    multiResVideo = it["multi-res-video"] as Boolean,
                    generateThumbnail = it["generate-thumbnail"] as Boolean
                )
            }
            (data.getValue("storage") as Map<*, *>).let {
                storage = StorageConfig(
                    data = File(it["data"] as String).canonicalPath,
                    logs = File(it["logs"] as String).canonicalPath
                )
            }
            (data.getValue("database") as Map<*, *>).let {
                database = DatabaseConfig(
                    type = it["type"] as String,
                    host = it["host"] as? String,
                    port = it["port"] as? Int,
                    username = it["username"] as? String,
                    password = it["password"] as? String,
                    databaseName = it["database-name"] as? String
                )
            }
        }
        catch(e: Exception) {
            println("Error reading configuration: ${e.message}")
            println("Please check 'config.example.yaml' for reference.")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}

data class SecurityConfig(
    val allowUserRegistration: Boolean,
    val proxy: ProxyConfig,
    val rateLimit: RateLimitConfig,
    val accessControl: AccessControlConfig
)

data class ProxyConfig(
    val enabled: Boolean,
    val realIpHeader: String,
    val trustedProxies: List<String>
)

data class RateLimitConfig(
    val enabled: Boolean,
    val intervalMinutes: Int,
    val maxRequests: Int,
    val useRealIp: Boolean
)

data class AccessControlConfig(
    val allowedHosts: AllowedHostsConfig,
    val blackList: BlackListConfig,
    val whiteList: WhiteListConfig
)

data class AllowedHostsConfig(
    val enabled: Boolean,
    val list: List<String>
)

data class BlackListConfig(
    val enabled: Boolean,
    val list: List<String>,
    val blockMessage: String
)

data class WhiteListConfig(
    val enabled: Boolean,
    val list: List<String>
)

data class MediaConfig(
    val multiResVideo: Boolean,
    val generateThumbnail: Boolean
)

data class StorageConfig(
    val data: String,
    val logs: String
)

data class DatabaseConfig(
    val type: String,
    val host: String?,
    val port: Int?,
    val username: String?,
    val password: String?,
    val databaseName: String?
)