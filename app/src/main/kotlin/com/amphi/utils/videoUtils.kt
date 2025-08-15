package com.amphi.utils

import io.vertx.core.json.JsonObject
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun getVideoResolution(videoPath: String): Pair<Int, Int>? {
    return try {
        val probeProcess = ProcessBuilder(
            "ffprobe", "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height",
            "-of", "json",
            videoPath
        ).start()
        val probeOutput = probeProcess.inputStream.bufferedReader().use { it.readText() }
        probeProcess.waitFor()

        val jsonObj = JsonObject(probeOutput)
        val streams = jsonObj.getJsonArray("streams")
        val stream = streams.getJsonObject(0)
        val width = stream.getInteger("width")
        val height = stream.getInteger("height")

        width to height
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun convertVideo(input: String, output: String, width: Int, height: Int) {
    val cmd = listOf(
        "ffmpeg", "-y", "-i", input,
        "-vf", "scale=w=$width:h=$height:force_original_aspect_ratio=decrease",
        "-c:a", "copy",
        output
    )
    ProcessBuilder(cmd).redirectErrorStream(true).start()
    val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
    val code = proc.waitFor()
    if (code != 0) throw RuntimeException("ffmpeg failed for $output")
}

fun isVideoExtension(fileExtension: String) : Boolean {
    val videoExtensions = setOf(
        "mp4", "mov", "avi", "wmv", "mkv", "flv", "webm",
        "mpeg", "mpg", "m4v", "3gp", "3g2", "f4v", "swf",
        "vob", "ts"
    )
    return fileExtension.lowercase() in videoExtensions
}

fun generateVideoThumbnail(input: String, output: String) {
    val targetHeight = 720
    val ffmpegCommand = listOf(
        "ffmpeg",
        "-y",
        "-i", input,
        "-ss", "00:00:05",
        "-vframes", "1",
        "-vf", "scale=-2:$targetHeight",
        output
    )

    try {
        val process = ProcessBuilder(ffmpegCommand)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            println("FFmpeg error: exit code $exitCode")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun generateImageThumbnail(input: String, output: String) {
    val original = ImageIO.read(File(input))
    val maxSize = 100

    val ratio = minOf(
        maxSize.toDouble() / original.width,
        maxSize.toDouble() / original.height
    )

    val width = (original.width * ratio).toInt()
    val height = (original.height * ratio).toInt()

    val resized = original.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = buffered.createGraphics()
    g.drawImage(resized, 0, 0, null)
    g.dispose()

    ImageIO.write(buffered, "jpg", File(output))
}