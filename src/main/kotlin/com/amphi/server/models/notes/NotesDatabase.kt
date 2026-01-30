package com.amphi.server.models.notes

import com.amphi.server.configs.AppConfig
import com.amphi.server.utils.setNullable
import io.vertx.core.json.JsonObject
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Duration
import java.time.Instant

class NotesDatabase(val userId: String) {

    private val connection = DriverManager.getConnection("jdbc:sqlite:${AppConfig.storage.data}/${userId}/notes/notes.db")

    fun close() {
        connection.close()
    }

    init {
        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS notes (
                                id TEXT PRIMARY KEY NOT NULL, 
                                content TEXT, 
                                created INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                is_folder BOOLEAN,
                                parent_id TEXT,
                                line_height INTEGER,
                                text_size INTEGER,
                                text_color INTEGER,
                                background_color INTEGER,
                                background TEXT,
                                title TEXT,
                                subtitle TEXT
                            );
                        """
                )
                statement.executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS themes (
                                id TEXT PRIMARY KEY NOT NULL,
                                title TEXT NOT NULL,
                                created INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                    
                                background_light INTEGER NOT NULL,
                                text_light INTEGER NOT NULL,
                                accent_light INTEGER NOT NULL,
                                card_light INTEGER NOT NULL,
                                floating_button_background_light INTEGER NOT NULL,
                                floating_button_icon_light INTEGER NOT NULL,
                    
                                background_dark INTEGER NOT NULL,
                                text_dark INTEGER NOT NULL,
                                accent_dark INTEGER NOT NULL,
                                card_dark INTEGER NOT NULL,
                                floating_button_background_dark INTEGER NOT NULL,
                                floating_button_icon_dark INTEGER NOT NULL
                            );
                        """
                )
                statement.executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS snapshots (
                                id TEXT PRIMARY KEY NOT NULL, 
                                note_id TEXT NOT NULL, 
                                content TEXT, 
                                created INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                is_folder BOOLEAN,
                                parent_id TEXT,
                                line_height INTEGER,
                                text_size INTEGER,
                                text_color INTEGER,
                                background_color INTEGER,
                                background TEXT,
                                title TEXT,
                                subtitle TEXT
                            );
                        """
                )
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

//    fun generatedId(): String {
//        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
//
//        while (true) {
//            val length = Random.nextInt(5) + 30
//            val id = (1..length)
//                .map { chars.random() }
//                .joinToString("")
//
//            val sql = "SELECT COUNT(*) FROM notes WHERE id = ?"
//            connection.prepareStatement(sql).use { stmt ->
//                stmt.setString(1, id)
//                val rs = stmt.executeQuery()
//                if (rs.next() && rs.getInt(1) == 0) {
//                    return id
//                }
//            }
//        }
//    }

    fun insertNote(note: Note) {
        val sql = """
                INSERT INTO notes (
                    id, content, created, modified, deleted, is_folder, parent_id,
                    line_height, text_size, text_color, background_color, background,
                    title, subtitle, permanently_deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                       ON CONFLICT(id) DO UPDATE SET
                  content = excluded.content,
                  modified = excluded.modified,
                  deleted = excluded.deleted,
                  is_folder = excluded.is_folder,
                  parent_id = excluded.parent_id,
                  line_height = excluded.line_height,
                  text_size = excluded.text_size,
                  text_color = excluded.text_color,
                  background_color = excluded.background_color,
                  background = excluded.background,
                  title = excluded.title,
                  subtitle = excluded.subtitle,
                  permanently_deleted = excluded.permanently_deleted,
                  permanently_deleted = NULL;
                """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setNote(note)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun insertLegacyTheme(id: String, jsonObject: JsonObject) {
        val sql = """
                INSERT INTO themes (
                    id, title, created, modified, background_light, text_light, accent_light,
                    card_light, floating_button_background_light, floating_button_icon_light, background_dark, text_dark,
                    accent_dark, card_dark, floating_button_background_dark, floating_button_icon_dark
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """.trimIndent()
        val statement = connection.prepareStatement(sql)
        statement.setLegacyTheme(id, jsonObject)
        statement.executeUpdate()
        statement.close()
    }

    fun getNotes(): List<Note> {
        val sql = "SELECT * FROM notes WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Note>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val note = Note.fromResultSet(resultSet)
            list.add(note)
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun getNoteById(id: String): Note? {
        val sql = "SELECT * FROM notes WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val note = Note.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return note
        } else {
            resultSet.close()
            statement.close()
            return null
        }

    }

    fun setNoteDeleted(id: String) {
        val sql = """
                UPDATE notes SET permanently_deleted = ? WHERE id = ?;
                """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setLong(1, Instant.now().toEpochMilli())
        preparedStatement.setString(2, id)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun getThemes(): List<NotesTheme> {
        val sql = "SELECT * FROM themes;"
        val list = mutableListOf<NotesTheme>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            list.add(NotesTheme.fromResultSet(resultSet))
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun insertTheme(theme: NotesTheme) {
        val sql = """
                INSERT INTO themes (
                    id, title, created, modified, background_light, text_light, accent_light,
                    card_light, floating_button_background_light, floating_button_icon_light, background_dark, text_dark,
                    accent_dark, card_dark, floating_button_background_dark, floating_button_icon_dark
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                  title = excluded.title,
                  created = excluded.created,
                  modified = excluded.modified,
                  background_light = excluded.background_light,
                  text_light = excluded.text_light,
                  accent_light = excluded.accent_light,
                  card_light = excluded.card_light,
                  floating_button_background_light = excluded.floating_button_background_light,
                  floating_button_icon_light = excluded.floating_button_icon_light,
                  background_dark = excluded.background_dark,
                  text_dark = excluded.text_dark,
                  accent_dark = excluded.accent_dark,
                  card_dark = excluded.card_dark,
                  floating_button_background_dark = excluded.floating_button_background_dark,
                  floating_button_icon_dark = excluded.floating_button_icon_dark;
                """.trimIndent()
        val statement = connection.prepareStatement(sql)
        statement.setTheme(theme)
        statement.executeUpdate()
        statement.close()
    }

    fun getThemeById(id: String): NotesTheme? {
        val sql = "SELECT * FROM themes WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val theme = NotesTheme.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return theme
        } else {
            resultSet.close()
            statement.close()
            return null
        }
    }

    fun deleteTheme(id: String) {
        val sql = "DELETE FROM themes WHERE id = ?;"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, id)
        preparedStatement.executeUpdate()
        preparedStatement.close()
    }

    fun deleteObsoleteNotes() {
        val threshold = Instant.now().minus(Duration.ofDays(30)).toEpochMilli()
        val sql = """
        DELETE FROM notes
        WHERE permanently_deleted IS NOT NULL
          AND permanently_deleted < ?
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, threshold)
            stmt.executeUpdate()
        }
    }
}

fun PreparedStatement.setNote(note: Note) {
    setString(1, note.id)
    setNullable(2, if (note.isFolder) null else note.content.toString(), Types.VARCHAR)
    setLong(3, note.created)
    setLong(4, note.modified)
    setNullable(5, note.deleted, Types.INTEGER)
    setBoolean(6, note.isFolder)
    setString(7, note.parentId)
    setNullable(8, note.lineHeight, Types.INTEGER)
    setNullable(9, note.textSize, Types.INTEGER)
    setNullable(10, note.textColor, Types.INTEGER)
    setNullable(11, note.backgroundColor, Types.INTEGER)
    setNullable(12, note.background, Types.VARCHAR)
    setNullable(13, note.title, Types.VARCHAR)
    setNullable(14, note.subtitle, Types.VARCHAR)
    setNull(15, Types.INTEGER)
}

fun PreparedStatement.setTheme(theme: NotesTheme) {
    setString(1, theme.id)
    setString(2, theme.title)
    setLong(3, theme.created)
    setLong(4, theme.modified)
    setLong(5, theme.lightColors.background)
    setLong(6, theme.lightColors.text)
    setLong(7, theme.lightColors.accent)
    setLong(8, theme.lightColors.card)
    setLong(9, theme.lightColors.floatingButtonBackground)
    setLong(10, theme.lightColors.floatingButtonIcon)
    setLong(11, theme.darkColors.background)
    setLong(12, theme.darkColors.text)
    setLong(13, theme.darkColors.accent)
    setLong(14, theme.darkColors.card)
    setLong(15, theme.darkColors.floatingButtonBackground)
    setLong(16, theme.darkColors.floatingButtonIcon)
}

fun PreparedStatement.setLegacyTheme(id: String, jsonObject: JsonObject) {
    setString(1, id)
    setString(2, jsonObject.getString("title"))
    setLong(3, jsonObject.getLong("created"))
    setLong(4, jsonObject.getLong("modified"))
    setLong(5, jsonObject.getLong("lightBackgroundColor"))
    setLong(6, jsonObject.getLong("lightTextColor"))
    setLong(7, jsonObject.getLong("lightAccentColor"))
    setLong(8, jsonObject.getLong("lightNoteBackgroundColor"))
    setLong(9, jsonObject.getLong("lightFloatingButtonBackground"))
    setLong(10, jsonObject.getLong("lightFloatingButtonIconColor"))
    setLong(11, jsonObject.getLong("darkBackgroundColor"))
    setLong(12, jsonObject.getLong("darkTextColor"))
    setLong(13, jsonObject.getLong("darkAccentColor"))
    setLong(14, jsonObject.getLong("darkNoteBackgroundColor"))
    setLong(15, jsonObject.getLong("darkFloatingButtonBackground"))
    setLong(16, jsonObject.getLong("darkFloatingButtonIconColor"))
}