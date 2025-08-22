package com.amphi.utils

import com.amphi.VERSION
import io.vertx.core.json.JsonObject
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.exitProcess

fun checkForUpdates() {
    try {
        val url = "https://api.github.com/repos/amphi2024/server/releases/latest"

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) {
            val json = JsonObject(response.body())
            val version = json.getString("tag_name")

            if(version != "v${VERSION}" && version != VERSION) {
                update()
            }
        }
    }
    catch (e: Exception) {
        println(e)
    }
}

fun update() {
    try {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://github.com/amphi2024/server-updater/releases/download/v1.0.0/updater.jar"))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() == 200) {
            FileOutputStream("updater.jar").use { fos ->
                fos.write(response.body())

                val javaHome = System.getProperty("java.home")
                val javaBin = "$javaHome/bin/java"
                ProcessBuilder(
                    javaBin, "-jar", "updater.jar"
                )
                    .inheritIO()
                    .start()

                exitProcess(0)
            }
        }
    } catch (e: Exception) {
        println(e)
    }
}