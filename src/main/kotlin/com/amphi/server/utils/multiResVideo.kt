package com.amphi.server.utils

import kotlin.concurrent.thread

fun generateMultiResVideo(filepath: String, outputPath1080p: String, outputPath720p: String) {
    thread {
        try {
            val resolution = getVideoResolution(filepath)
            if (resolution != null) {
                val (width, height) = resolution

                if (width >= 3840 || height >= 2160) {
                    convertVideo(filepath, outputPath1080p, 1920, 1080)
                    convertVideo(filepath, outputPath720p, 1280, 720)
                } else if (width >= 1920 || height >= 1080) {
                    convertVideo(filepath, outputPath1080p, 1920, 1080)
                    convertVideo(filepath, outputPath720p, 1280, 720)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}