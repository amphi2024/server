package com.amphi

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ServerSettings {

    var port = 8000
    var openRegistration = true
    var loginExpirationPeriod = 30
    var blockMessage = "you are blocked"
    var whitelistOnly = false
    var httpsOnly = false
    var keystorePath = "path/to/your/keystore.p12"
    var keystorePassword = "your-password"
    var rateLimitIntervalMinutes = 10
    var rateLimitMaxRequests = 450

    private var logs = StringBuilder()
    private val whitelist = mutableListOf<String>()
    private val blacklist = mutableListOf<String>()

    init {
        try {
            val file = File("settings.txt")

            if(!file.exists()) {
                saveSettings()
            }
            else {
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
                        "https-only" -> {
                            httpsOnly = second.toBoolean()
                        }
                        "keystore-path" -> {
                            keystorePath = second
                        }
                        "keystore-password" -> {
                            keystorePassword = second
                        }
                    }
               }

            }
            getWhiteList()
            getBlackList()
        }
        catch(e:Exception) {
            println("There was an error reading the configuration file. Some settings may not be applied.")
            saveSettings()
        }
        finally {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val file = File("settings.txt")
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("port:$port")
        stringBuilder.appendLine("open-registration:$openRegistration")
        stringBuilder.appendLine("login-expiration-period:$loginExpirationPeriod")
        stringBuilder.appendLine("block-message:$blockMessage")
        stringBuilder.appendLine("whitelist-only:$whitelistOnly")
        stringBuilder.appendLine("rate-limit-interval-minutes:$rateLimitIntervalMinutes")
        stringBuilder.appendLine("rate-limit-max-requests:$rateLimitMaxRequests")
        stringBuilder.appendLine("https-only:$httpsOnly")
        stringBuilder.appendLine("keystore-path:$keystorePath")
        stringBuilder.append("keystore-password:$keystorePassword")
        file.writeText(stringBuilder.toString())
    }

    private fun getBlackList() {
        try {
            val blackListFile = File("black-list.txt")
            if(!blackListFile.exists()) {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("?.?.?.?")
                stringBuilder.append("?.?.*")
                blackListFile.writeText(stringBuilder.toString())
            }
            else {
                blackListFile.forEachLine { line ->
                    val string = line.replace("*", "")
                    blacklist.add(string)
                }
            }
        }
        catch (e: Exception) {
            println("Unable to read the blacklist file.")
        }
    }

    private fun getWhiteList() {
        try {
            val whiteListFile = File("white-list.txt")
            if(!whiteListFile.exists()) {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("?.?.?.?")
                stringBuilder.append("?.?.*")
                whiteListFile.writeText(stringBuilder.toString())
            }
            else {
                whiteListFile.forEachLine { line ->
                    val string = line.replace("*", "")
                    whitelist.add(string)
                }
            }
        }
        catch (e: Exception) {
            println("Unable to read the whitelist file.")
        }
    }

    fun writeLog(message: String) {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val string = "[${currentDateTime}] $message"
        println(string)
        logs.appendLine(string)
        if(logs.lines().size > 100) {
            saveLogs()
        }
    }
    fun saveLogs() {
//        val file = File("logs.txt")
//        file.appendText(logs.toString())
    }

    fun inBlackList(ipAddress: String): Boolean {
        var result = false
        for(item in blacklist) {
            if(ipAddress.contains(item)) {
                result = true
                break
            }
        }
        return result
    }

    fun inWhiteList(ipAddress: String): Boolean {
        var result = false
        for(item in whitelist) {
            if(ipAddress.contains(item)) {
                result = true
                break
            }
        }
        return result
    }
}