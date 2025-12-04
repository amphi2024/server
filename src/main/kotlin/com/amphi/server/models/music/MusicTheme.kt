package com.amphi.server.models.music

import io.vertx.core.json.JsonObject
import java.sql.ResultSet

class MusicTheme(
    val id: String,
    val title: String,
    val modified: Long,
    val created: Long,
    val lightColors: MusicThemeColors,
    val darkColors: MusicThemeColors
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : MusicTheme {
            return MusicTheme(
                id = resultSet.getString("id"),
                title = resultSet.getString("title"),
                modified = resultSet.getLong("modified"),
                created = resultSet.getLong("created"),
                lightColors = MusicThemeColors(
                    background = resultSet.getLong("background_light"),
                    text = resultSet.getLong("text_light"),
                    accent = resultSet.getLong("accent_light"),
                    card = resultSet.getLong("card_light")
                ),
                darkColors = MusicThemeColors(
                    background = resultSet.getLong("background_dark"),
                    text = resultSet.getLong("text_dark"),
                    accent = resultSet.getLong("accent_dark"),
                    card = resultSet.getLong("card_dark")
                )
            )
        }
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.put("id", id)
        jsonObject.put("title", title)
        jsonObject.put("modified", modified)
        jsonObject.put("created", created)
        jsonObject.put("background_light", lightColors.background)
        jsonObject.put("text_light", lightColors.text)
        jsonObject.put("accent_light", lightColors.accent)
        jsonObject.put("card_light", lightColors.card)
        jsonObject.put("background_dark", darkColors.background)
        jsonObject.put("text_dark", darkColors.text)
        jsonObject.put("accent_dark", darkColors.accent)
        jsonObject.put("card_dark", darkColors.card)

        return jsonObject
    }
}

data class MusicThemeColors(
    val background: Long,
    val text: Long,
    val accent: Long,
    val card: Long
)