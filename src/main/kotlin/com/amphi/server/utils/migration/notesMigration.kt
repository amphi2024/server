package com.amphi.server.utils.migration

import com.amphi.server.models.notes.Note
import com.amphi.server.models.notes.NotesDatabase
import com.amphi.server.trashService
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun migrateNotes(userId: String, userDirectory: File) {
    moveThemesInOldVersion(userDirectory)
    moveColorsInOldVersion(userDirectory)

    val notesDirectory = File(userDirectory,"notes/notes")

    if(notesDirectory.listFiles()?.isEmpty() == true) {
        notesDirectory.delete()
        return
    }
    if (!notesDirectory.exists()) {
        return
    }

    val database = NotesDatabase(userDirectory.name)

    notesDirectory.listFiles()?.forEach { file ->

        if (file.isFile && file.extension != "db") {
            val note = Note.legacy(file)
            database.insertNote(note)

            moveOldNoteFileToTrash(file = file, userId = userId)

            moveOldAttachments(file = file, userId = userId, newId = note.id)
        }
    }

    migrateThemes(userDirectory, database)

    database.close()

}

private fun migrateThemes(userDirectory: File, database: NotesDatabase) {
    val themesDirectory = File(userDirectory,"notes/themes")

    if(themesDirectory.listFiles()?.isEmpty() == true) {
        themesDirectory.delete()
        return
    }
    if (!themesDirectory.exists()) {
        return
    }

    val targetDirectory = File(userDirectory, "trash/notes/themes")
    if(!targetDirectory.exists()) {
        targetDirectory.mkdirs()
    }

    themesDirectory.listFiles()?.forEach { file ->

        if (file.isFile && file.extension != "db") {
            try {
                val jsonObject = JsonObject(file.readText())
                database.insertLegacyTheme(file.nameWithoutExtension, jsonObject)
            }
            catch (e: Exception) {
                println(e)
            }

            Files.move(
                file.toPath(),
                Paths.get("${targetDirectory.path}/${file.name}"),
                StandardCopyOption.REPLACE_EXISTING
            )
            trashService.notifyFileDelete("${targetDirectory.path}/${file.name}")

        }
    }
}

private fun moveThemesInOldVersion(userDirectory: File) {
    val themesInOldVersion = File(userDirectory, "notes/notes/themes")
    if (themesInOldVersion.exists()) {
        val themesDir = File(userDirectory,"notes/themes")
        if (!themesDir.exists()) {
            themesInOldVersion.renameTo(themesDir)
        } else if (themesDir.isDirectory) {
            themesInOldVersion.listFiles()?.forEach { themeFile ->
                themeFile.renameTo(File(userDirectory,"notes/themes/${themeFile.name}"))
            }
            themesInOldVersion.delete()
        } else if (themesDir.isFile) {
            themesDir.delete()
        }
    }
}

private fun moveColorsInOldVersion(userDirectory: File) {
    val colorsInOldVersion = File(userDirectory, "notes/notes/colors")
    if (colorsInOldVersion.exists()) {
        val colorsFile = File(userDirectory,"notes/colors")
        if (!colorsFile.exists()) {
            colorsInOldVersion.renameTo(colorsFile)
        } else {
            colorsInOldVersion.delete()
        }
    }
}