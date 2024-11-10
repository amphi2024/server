package com.amphi

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import java.io.File

object UpdateService {

    private const val CENTRAL_SERVER_ADDRESS = "127.0.0.1"
    private const val CENTRAL_SERVER_PORT = 5050
    private const val USE_SSL = false

    private const val VERSION = "0.0.0"

    fun checkForUpdate(vertx: Vertx, update: () -> Unit) {
        val client = WebClient.create(vertx)
//
        println("checking for update.....")
        // https = 443,  http = 80
        client.get(CENTRAL_SERVER_PORT, CENTRAL_SERVER_ADDRESS, "/server/version")
            .ssl(USE_SSL)  // HTTPS 연결 사용
            .send { response ->
                if (response.succeeded()) {
                    val result = response.result()
                    val latestVersion = result.bodyAsString()
                    if(VERSION != latestVersion) {
                        downloadServer(client)
                        update()
                    }
                } else {
                    println("Update Check Request failed: ${response.cause().message}")
                }
            }
    }

    private fun downloadServer(client: WebClient) {
        client.get(CENTRAL_SERVER_PORT, CENTRAL_SERVER_ADDRESS, "/server/latest")
            .ssl(USE_SSL)  // HTTPS 연결 사용
            .send { response ->
                if (response.succeeded()) {
                    val result = response.result()
                    val buffer = result.bodyAsBuffer()
                    val file = File("server_latest.jar")
                    file.writeBytes(buffer.bytes)
                }
            }
    }
}