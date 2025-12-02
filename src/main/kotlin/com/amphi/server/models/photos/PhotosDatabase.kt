package com.amphi.server.models.photos

import com.amphi.server.utils.setNullable
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Types

class PhotosDatabase(val userId: String) {

    private val connection = DriverManager.getConnection("jdbc:sqlite:users/${userId}/photos/photos.db")

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
                    tags = excluded.tags;
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
                    note = excluded.note;
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
}