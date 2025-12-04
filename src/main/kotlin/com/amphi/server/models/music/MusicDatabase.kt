package com.amphi.server.models.music

import com.amphi.server.utils.setNullable
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Duration
import java.time.Instant

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

    fun getPlaylists(): List<Playlist> {
        val sql = "SELECT * FROM albums WHERE permanently_deleted IS NULL;"
        val list = mutableListOf<Playlist>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            val playlist = Playlist.fromResultSet(resultSet)
            list.add(playlist)
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun getSongById(id: String): Song? {
        val sql = "SELECT * FROM songs WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val song = Song.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return song
        } else {
            resultSet.close()
            statement.close()
            return null
        }
    }

    fun getArtistById(id: String): Artist? {
        val sql = "SELECT * FROM artists WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val artist = Artist.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return artist
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

    fun getPlaylistById(id: String): Playlist? {
        val sql = "SELECT * FROM playlists WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val playlist = Playlist.fromResultSet(resultSet)
            resultSet.close()
            statement.close()
            return playlist
        } else {
            resultSet.close()
            statement.close()
            return null
        }
    }

    fun insertSong(song: Song, onComplete: ((result: Int) -> Unit)? = null) {
        val sql = """
                    INSERT INTO songs (
                        id, title, genres, artist_ids, album_id, added, modified,
                        deleted, composer_ids, lyricist_ids, arranger_ids,
                        producer_ids, archived, released, track_number, disc_number,
                        description, files, featured_artist_ids
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?
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
                        featured_artist_ids = excluded.featured_artist_ids,
                        permanently_deleted = NULL;
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
        setNullable(19, song.featuredArtistIds?.toString(), Types.VARCHAR)
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
                        description = excluded.description,
                        permanently_deleted = NULL;
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
                        description = excluded.description,
                        permanently_deleted = NULL;
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
                        note = excluded.note,
                        permanently_deleted = NULL;
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

    fun getThemes(): List<MusicTheme> {
        val sql = "SELECT * FROM themes;"
        val list = mutableListOf<MusicTheme>()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            list.add(MusicTheme.fromResultSet(resultSet))
        }

        resultSet.close()
        statement.close()

        return list
    }

    fun insertTheme(theme: MusicTheme) {
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

    private fun PreparedStatement.setTheme(theme: MusicTheme) {
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

    fun getThemeById(id: String): MusicTheme? {
        val sql = "SELECT * FROM themes WHERE id = ?;"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            val theme = MusicTheme.fromResultSet(resultSet)
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

    fun setSongDeleted(id: String) = setItemDeleted("songs", id)
    fun setArtistDeleted(id: String) = setItemDeleted("artists", id)
    fun setAlbumDeleted(id: String) = setItemDeleted("albums", id)
    fun setPlaylistDeleted(id: String) = setItemDeleted("playlists", id)

    private fun setItemDeleted(table: String, id: String) {
        val sql = "UPDATE $table SET permanently_deleted = ? WHERE id = ?;"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, Instant.now().toEpochMilli())
            stmt.setString(2, id)
            stmt.executeUpdate()
        }
    }

    fun deleteObsoleteItems() {
        deleteObsoleteItemsForTable("songs")
        deleteObsoleteItemsForTable("artists")
        deleteObsoleteItemsForTable("albums")
        deleteObsoleteItemsForTable("playlists")
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