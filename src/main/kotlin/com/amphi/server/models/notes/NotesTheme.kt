package com.amphi.server.models.notes

import io.vertx.core.json.JsonObject
import java.sql.ResultSet

class NotesTheme(
    val id: String,
    val title: String,
    val modified: Long,
    val created: Long,
    val lightColors: NotesThemeColors,
    val darkColors: NotesThemeColors
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet) : NotesTheme {
            return NotesTheme(
                id = resultSet.getString("id"),
                title = resultSet.getString("title"),
                modified = resultSet.getLong("modified"),
                created = resultSet.getLong("created"),
                lightColors = NotesThemeColors(
                    background = resultSet.getLong("background_light"),
                    text = resultSet.getLong("text_light"),
                    accent = resultSet.getLong("accent_light"),
                    card = resultSet.getLong("card_light"),
                    floatingButtonBackground = resultSet.getLong("floating_button_background_light"),
                    floatingButtonIcon = resultSet.getLong("floating_button_icon_light")
                ),
                darkColors = NotesThemeColors(
                    background = resultSet.getLong("background_dark"),
                    text = resultSet.getLong("text_dark"),
                    accent = resultSet.getLong("accent_dark"),
                    card = resultSet.getLong("card_dark"),
                    floatingButtonBackground = resultSet.getLong("floating_button_background_dark"),
                    floatingButtonIcon = resultSet.getLong("floating_button_icon_dark")
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
        jsonObject.put("floating_button_background_light", lightColors.floatingButtonBackground)
        jsonObject.put("floating_button_icon_light", lightColors.floatingButtonIcon)
        jsonObject.put("background_dark", darkColors.background)
        jsonObject.put("text_dark", darkColors.text)
        jsonObject.put("accent_dark", darkColors.accent)
        jsonObject.put("card_dark", darkColors.card)
        jsonObject.put("floating_button_background_dark", darkColors.floatingButtonBackground)
        jsonObject.put("floating_button_icon_dark", darkColors.floatingButtonIcon)

        return jsonObject
    }
}

data class NotesThemeColors(
    val background: Long,
    val text: Long,
    val accent: Long,
    val card: Long,
    val floatingButtonBackground: Long,
    val floatingButtonIcon: Long
)