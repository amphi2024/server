package com.amphi.server.models.music

import com.amphi.server.utils.setNullable
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Types

class MusicDatabase(val userId: String) {

    private val connection = DriverManager.getConnection("jdbc:sqlite:users/${userId}/music/music.db")

    fun close() {
        connection.close()
    }

    init {
        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                             CREATE TABLE IF NOT EXISTS songs (
                                id TEXT PRIMARY KEY NOT NULL, 
                                title TEXT NOT NULL,
                                genres TEXT,
                                artist_ids TEXT,
                                album_id TEXT,
                                added INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                composer_ids TEXT,
                                lyricist_ids TEXT,
                                arranger_ids TEXT,
                                producer_ids TEXT,
                                archived BOOLEAN,
                                released INTEGER,
                                track_number INTEGER,
                                disc_number INTEGER,
                                description TEXT,
                                files TEXT NOT NULL,
                                visibility TEXT,
                                featured_artist_ids TEXT
                              );
                        """
                )
                statement.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS artists (
                                id TEXT PRIMARY KEY NOT NULL, 
                                name TEXT NOT NULL,
                                images TEXT,
                                members TEXT,
                                added INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                debut INTEGER,
                                country TEXT,
                                description TEXT
                              );
                        """
                )
                statement.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS albums (
                                id TEXT PRIMARY KEY NOT NULL, 
                                title TEXT NOT NULL,
                                covers TEXT,
                                genres TEXT,
                                artist_ids TEXT,
                                added INTEGER NOT NULL,
                                modified INTEGER NOT NULL,
                                deleted INTEGER,
                                permanently_deleted INTEGER,
                                released INTEGER,
                                description TEXT
                              );
                        """
                )
                statement.executeUpdate(
                    """
                         CREATE TABLE IF NOT EXISTS playlists (
                            id TEXT PRIMARY KEY NOT NULL, 
                            title TEXT NOT NULL,
                            songs TEXT NOT NULL,
                            created INTEGER NOT NULL,
                            modified INTEGER NOT NULL,
                            deleted INTEGER,
                            permanently_deleted INTEGER,
                            thumbnails TEXT,
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

    fun getSongs(): List<Song> {
        val sql = "SELECT * FROM songs WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Song>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val song = Song.fromResultSet(resultSet)
            list.add(song)
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun getArtists(): List<Artist> {
        val sql = "SELECT * FROM artists WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Artist>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val artist = Artist.fromResultSet(resultSet)
            list.add(artist)
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

    fun insertSong(song: Song, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                    INSERT INTO songs (
                        id, title, genres, artist_ids, album_id, added, modified,
                        deleted, composer_ids, lyricist_ids, arranger_ids,
                        producer_ids, archived, released, track_number, disc_number,
                        description, files, visibility, featured_artist_ids
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?
                    )
                    ON CONFLICT(id) DO UPDATE SET
                        title = excluded.title,
                        genres = excluded.genres,
                        artist_ids = excluded.artist_ids,
                        album_id = excluded.album_id,
                        added = excluded.added,
                        modified = excluded.modified,
                        deleted = excluded.deleted,
                        composer_ids = excluded.composer_ids,
                        lyricist_ids = excluded.lyricist_ids,
                        arranger_ids = excluded.arranger_ids,
                        producer_ids = excluded.producer_ids,
                        archived = excluded.archived,
                        released = excluded.released,
                        track_number = excluded.track_number,
                        disc_number = excluded.disc_number,
                        description = excluded.description,
                        files = excluded.files,
                        visibility = excluded.visibility,
                        featured_artist_ids = excluded.featured_artist_ids;
                    """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setSong(song)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    fun PreparedStatement.setSong(song: Song) {
        setString(1, song.id)
        setString(2, song.title.toString())
        setNullable(3, song.genres?.toString(), Types.VARCHAR)
        setNullable(4, song.artistIds?.toString(), Types.VARCHAR)
        setNullable(5, song.albumId, Types.VARCHAR)
        setLong(6, song.added)
        setLong(7, song.modified)
        setNullable(8, song.deleted, Types.INTEGER)
        setNullable(9, song.composerIds?.toString(), Types.VARCHAR)
        setNullable(10, song.lyricistIds?.toString(), Types.VARCHAR)
        setNullable(11, song.arrangerIds?.toString(), Types.VARCHAR)
        setNullable(12, song.producerIds?.toString(), Types.VARCHAR)
        setBoolean(13, song.archived)
        setObject(14, song.released)
        setObject(15, song.trackNumber)
        setObject(16, song.discNumber)
        setString(17, song.description)
        setString(18, song.files.toString())
        setString(19, null)
        setNullable(20, song.featuredArtistIds?.toString(), Types.VARCHAR)
    }

    fun insertArtist(artist: Artist, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                    INSERT INTO artists (
                        id, name, images, members, added, modified,
                        deleted, debut, country, description
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?
                    )
                    ON CONFLICT(id) DO UPDATE SET
                        name = excluded.name,
                        images = excluded.images,
                        members = excluded.members,
                        added = excluded.added,
                        modified = excluded.modified,
                        deleted = excluded.deleted,
                        debut = excluded.debut,
                        country = excluded.country,
                        description = excluded.description;
                    """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setArtist(artist)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    fun PreparedStatement.setArtist(artist: Artist) {
        setString(1, artist.id)
        setString(2, artist.name.toString())
        setString(3, artist.images.toString())
        setNullable(4, artist.members?.toString(), Types.VARCHAR)
        setLong(5, artist.added)
        setLong(6, artist.modified)
        setNullable(7, artist.deleted, Types.INTEGER)
        setNullable(8, artist.debut, Types.INTEGER)
        setNullable(9, artist.country, Types.VARCHAR)
        setNullable(10, artist.description, Types.VARCHAR)
    }

    fun insertAlbum(album: Album, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                    INSERT INTO albums (
                        id, title, covers, genres, artist_ids, added, modified,
                        deleted, released, description
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?
                    )
                    ON CONFLICT(id) DO UPDATE SET
                        title = excluded.title,
                        covers = excluded.covers,
                        genres = excluded.genres,
                        artist_ids = excluded.artist_ids,
                        added = excluded.added,
                        modified = excluded.modified,
                        deleted = excluded.deleted,
                        released = excluded.released,
                        description = excluded.description;
                    """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setAlbum(album)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    fun PreparedStatement.setAlbum(album: Album) {
        setString(1, album.id)
        setString(2, album.title.toString())
        setNullable(3, album.covers?.toString(), Types.VARCHAR)
        setNullable(4, album.genres?.toString(), Types.VARCHAR)
        setNullable(5, album.artistIds?.toString(), Types.VARCHAR)
        setLong(6, album.added)
        setLong(7, album.modified)
        setNullable(8, album.deleted, Types.INTEGER)
        setNullable(9, album.released, Types.INTEGER)
        setNullable(10, album.description, Types.VARCHAR)
    }


    fun insertPlaylist(playlist: Playlist, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                    INSERT INTO playlists (
                        id, title, songs, created, modified,
                        deleted, thumbnails, note
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?
                    )
                    ON CONFLICT(id) DO UPDATE SET
                        title = excluded.title,
                        songs = excluded.songs,
                        created = excluded.created,
                        modified = excluded.modified,
                        deleted = excluded.deleted,
                        thumbnails = excluded.thumbnails,
                        note = excluded.note;
                    """.trimIndent()
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setPlaylist(playlist)
        val result = preparedStatement.executeUpdate()
        onComplete?.invoke(result)
        preparedStatement.close()
    }

    fun PreparedStatement.setPlaylist(playlist: Playlist) {
        setString(1, playlist.id)
        setString(2, playlist.title)
        setString(3, playlist.songs.toString())
        setLong(4, playlist.created)
        setLong(5, playlist.modified)
        setNullable(6, playlist.deleted, Types.INTEGER)
        setNullable(7, playlist.thumbnails.toString(), Types.INTEGER)
        setNullable(8, playlist.note, Types.VARCHAR)
    }
}