package com.amphi.server

import com.amphi.server.models.CloudAppDatabase
import com.amphi.server.models.Note
import com.amphi.server.models.NoteContent
import com.amphi.server.models.TrashLog
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

object ServerFileUtils {

    //* Delete files in trash, Delete Unused files from notes, etc */
    fun organizeFiles() {
        val users = File("users")
        val trashLogs = ServerDatabase.getTrashLogs()
        if(users.exists()) {
            users.listFiles()?.forEach { userDirectory ->
                deleteObsoleteCloudFiles(userDirectory)

                val trashes = File("users/${userDirectory.name}/trashes")
                emptyTrash(trashes, trashLogs)

                val themesInOldVersion = File("users/${userDirectory.name}/notes/notes/themes")
                val colorsInOldVersion =  File("users/${userDirectory.name}/notes/notes/colors")
                if(themesInOldVersion.exists()) {
                    val themesDir = File("users/${userDirectory.name}/notes/themes")
                    if(!themesDir.exists()) {
                        themesInOldVersion.renameTo(themesDir)
                    }
                    else if(themesDir.isDirectory) {
                        themesInOldVersion.listFiles()?.forEach { themeFile ->
                            themeFile.renameTo(File("users/${userDirectory.name}/notes/themes/${themeFile.name}"))
                        }
                        themesInOldVersion.delete()
                    }
                    else if(themesDir.isFile) {
                        themesDir.delete()
                    }
                }
                if(colorsInOldVersion.exists()) {
                    val colorsFile = File("users/${userDirectory.name}/notes/colors")
                    if(!colorsFile.exists()) {
                        colorsInOldVersion.renameTo(colorsFile)
                    }
                    else {
                        colorsInOldVersion.delete()
                    }
                }

                val notes = File("users/${userDirectory.name}/notes/notes")
                notes.listFiles()?.forEach { fileInNotes ->

                    if(fileInNotes.isDirectory) {
                        val noteFile = File("${fileInNotes.path}.note")
                        if(!noteFile.exists()) {
                            fileInNotes.delete()
                            /*
                            * Ex:
                            * A123.note (file) is not exist
                            * A123 (directory) is exist
                            * then delete A123 (directory)
                            * */
                        }
                    }
                    else {
                        if(fileInNotes.extension == "note") { // delete obsolete media files of note
                            val note = Note(fileInNotes)
                            val mediaContents = mutableListOf<NoteContent>()
                            val imageFiles =  File("users/${userDirectory.name}/notes/notes/${note.name}/images").listFiles()?.toMutableList()
                            val videoFiles =  File("users/${userDirectory.name}/notes/notes/${note.name}/videos").listFiles()?.toMutableList()
                            val audioFiles =  File("users/${userDirectory.name}/notes/notes/${note.name}/audio").listFiles()?.toMutableList()

                            note.contents.forEach { content ->
                                when(content.type) {
                                    "img", "video", "audio" -> mediaContents.add(content)
                                }
                            }
                           deleteObsoleteMediaFiles(imageFiles, mediaContents)
                            deleteObsoleteMediaFiles(videoFiles, mediaContents)
                            deleteObsoleteMediaFiles(audioFiles, mediaContents)
                        }
                    }
                }
            }
        }
    }

    private fun deleteObsoleteMediaFiles(files: MutableList<File>?, mediaContents: MutableList<NoteContent>) {
        if(files != null) {
            val imageIterator = files.listIterator()
            while (imageIterator.hasNext()) {
                val imageFile = imageIterator.next()
                val mediaIterator = mediaContents.listIterator()
                while (mediaIterator.hasNext()) {
                    if (mediaIterator.next().value == imageFile.name) {
                        mediaIterator.remove()
                        imageIterator.remove()
                        break
                    }
                }
            }
            files.forEach { file ->
                file.delete()
            }
        }
    }

    private fun emptyTrash(trashes : File, trashLogs : List<TrashLog>) {
        trashes.listFiles()?.forEach { file ->
            if(file.isFile) {
                trashLogs.forEach { trashLog ->
                    if(trashLog.path == file.path) {
                        val period = Duration.between(
                            trashLog.timeStamp.atZone(ZoneId.systemDefault()).toInstant(),
                            LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
                        )
                        if (period.toDays() > 30) {
                            file.delete()
                            ServerDatabase.deleteTrashLog(trashLog.path)
                        }
                    }
                }
            }
            else if(file.isDirectory) {
                emptyTrash(file, trashLogs)
            }
        }
    }

    private fun deleteObsoleteCloudFiles(userDirectory: File) {
        val cloudDBFile = File("users/${userDirectory.name}/cloud/cloud.db")
        if(cloudDBFile.exists()) {
            val database = CloudAppDatabase(userDirectory.name)
            database.permanentlyDeleteObsoleteFiles()
            database.close()
        }
    }
}