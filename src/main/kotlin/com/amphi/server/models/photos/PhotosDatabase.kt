package com.amphi.server.models.photos

import com.amphi.server.configs.AppConfig
import com.amphi.server.utils.setNullable
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Duration
import java.time.Instant

class PhotosDatabase(val userId: String) {

    private val connection = DriverManager.getConnection("jdbc:sqlite:${AppConfig.storage.data}/${userId}/photos/photos.db")

    fun close() {
        connection.close()
    }

    init {
        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                             CREATE TABLE IF NOT EXISTS photos (
                                id TEXT PRIMARY KEY NOT NULL, 
                                title TEXT NOT NULL,
                                created INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                date INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                mime_type TEXT NOT NULL,
                                sha256 TEXT NOT NULL,
                                note TEXT,
                                tags TEXT
                              );
                        """
                )
                statement.executeUpdate(
                    """
                             CREATE TABLE IF NOT EXISTS albums (
                                id TEXT PRIMARY KEY NOT NULL, 
                                title TEXT NOT NULL,
                                created INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                photos TEXT NOT NULL,
                                cover_photo_index INTEGER,
                                note TEXT
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
                    
                                background_dark INTEGER NOT NULL,
                                text_dark INTEGER NOT NULL,
                                accent_dark INTEGER NOT NULL,
                                card_dark INTEGER NOT NULL
                              );
                        """
                )
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun getPhotos(): List<Photo> {
        val sql = "SELECT * FROM photos WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Photo>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val photos = Photo.fromResultSet(resultSet)
            list.add(photos)
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun getAlbums(): List<Album> {
        val sql = "SELECT * FROM albums WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Album>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val album = Album.fromResultSet(resultSet)
            list.add(album)
        }

        resultSet.close()
        statement.close()

        return list
    }


    fun getPhotoById(id: String): Photo? {
        val sql = "SELECT * FROM photos WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val photo = Photo.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return photo
        } else {
            resultSet.close()
            statement.close()
            return null
        }
    }

    fun getAlbumById(id: String): Album? {
        val sql = "SELECT * FROM albums WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val album = Album.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return album
        } else {
            resultSet.close()
            statement.close()
            return null
        }
    }

    fun insertPhoto(photo: Photo, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                INSERT INTO photos (
                    id, title, created, modified, date,
                    deleted, mime_type, sha256, note, tags
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    created = excluded.created,
                    modified = excluded.modified,
                    date = excluded.date,
                    deleted = excluded.deleted,
                    mime_type = excluded.mime_type,
                    sha256 = excluded.sha256,
                    note = excluded.note,
                    tags = excluded.tags,
                    permanently_deleted = NULL;
                """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setPhoto(photo)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    private fun PreparedStatement.setPhoto(photo: Photo) {
        setString(1, photo.id)
        setString(2, photo.title)
        setLong(3, photo.created)
        setLong(4, photo.modified)
        setLong(5, photo.date)
        setNullable(6, photo.deleted, Types.INTEGER)
        setString(7, photo.mimeType)
        setString(8, photo.sha256)
        setNullable(9, photo.note, Types.VARCHAR)
        setNullable(10, photo.tags?.toString(), Types.VARCHAR)
    }

    fun insertAlbum(album: Album, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                INSERT INTO albums (
                    id, title, created, modified,
                    deleted, photos, cover_photo_index, note
                ) VALUES (
                    ?, ?, ?, ?,
                    ?, ?, ?, ?
                )
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    created = excluded.created,
                    modified = excluded.modified,
                    deleted = excluded.deleted,
                    photos = excluded.photos,
                    cover_photo_index = excluded.cover_photo_index,
                    note = excluded.note,
                    permanently_deleted = NULL;
                """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setAlbum(album)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    private fun PreparedStatement.setAlbum(album: Album) {
        setString(1, album.id)
        setString(2, album.title)
        setLong(3, album.created)
        setLong(4, album.modified)
        setNullable(5, album.deleted, Types.INTEGER)
        setString(6, album.photos.toString())
        setNullable(7, album.coverPhotoIndex, Types.INTEGER)
        setNullable(8, album.note, Types.VARCHAR)
    }

    fun getThemes(): List<PhotosTheme> {
        val sql = "SELECT * FROM themes;"
        val list = mutableListOf<PhotosTheme>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            list.add(PhotosTheme.fromResultSet(resultSet))
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun insertTheme(theme: PhotosTheme) {
        val sql = """
                INSERT INTO themes (
                    id, title, created, modified, background_light, text_light, accent_light,
                    card_light, background_dark, text_dark,
                    accent_dark, card_dark
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                  title = excluded.title,
                  created = excluded.created,
                  modified = excluded.modified,
                  background_light = excluded.background_light,
                  text_light = excluded.text_light,
                  accent_light = excluded.accent_light,
                  card_light = excluded.card_light,
                  background_dark = excluded.background_dark,
                  text_dark = excluded.text_dark,
                  accent_dark = excluded.accent_dark,
                  card_dark = excluded.card_dark;
                """.trimIndent()
        val statement = connection.prepareStatement(sql)
        statement.setTheme(theme)
        statement.executeUpdate()
        statement.close()
    }

    private fun PreparedStatement.setTheme(theme: PhotosTheme) {
        setString(1, theme.id)
        setString(2, theme.title)
        setLong(3, theme.created)
        setLong(4, theme.modified)
        setLong(5, theme.lightColors.background)
        setLong(6, theme.lightColors.text)
        setLong(7, theme.lightColors.accent)
        setLong(8, theme.lightColors.card)
        setLong(9, theme.darkColors.background)
        setLong(10, theme.darkColors.text)
        setLong(11, theme.darkColors.accent)
        setLong(12, theme.darkColors.card)
    }

    fun getThemeById(id: String): PhotosTheme? {
        val sql = "SELECT * FROM themes WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val theme = PhotosTheme.fromResultSet(resultSet)
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

    fun setPhotoDeleted(id: String) = setItemDeleted("photos", id)
    fun setAlbumDeleted(id: String) = setItemDeleted("albums", id)

    private fun setItemDeleted(table: String, id: String) {
        val sql = "UPDATE $table SET permanently_deleted = ? WHERE id = ?;"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, Instant.now().toEpochMilli())
            stmt.setString(2, id)
            stmt.executeUpdate()
        }
    }

    fun deleteObsoleteItems() {
        deleteObsoleteItemsForTable("photos")
        deleteObsoleteItemsForTable("albums")
    }

    private fun deleteObsoleteItemsForTable(table: String) {
        val threshold = Instant.now().minus(Duration.ofDays(30)).toEpochMilli()
        val sql = """
        DELETE FROM $table
        WHERE permanently_deleted IS NOT NULL
          AND permanently_deleted < ?
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, threshold)
            stmt.executeUpdate()
        }
    }
}