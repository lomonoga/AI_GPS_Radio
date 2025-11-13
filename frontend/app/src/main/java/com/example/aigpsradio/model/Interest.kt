package com.example.aigpsradio.model

/**
 * Модель данных для категории интересов пользователя
 */

data class Interest(
    val id: String,
    val title: String,
    val icon: Int
)

object InterestsData {
    fun getAvailableInterests(): List<Interest> {
        return listOf(
            Interest(
                id = "nature",
                title = "Природа",
                icon = android.R.drawable.ic_menu_compass // Замените на R.drawable.ic_nature
            ),
            Interest(
                id = "architecture",
                title = "Архитектура",
                icon = android.R.drawable.ic_menu_info_details // Замените на R.drawable.ic_architecture
            ),
            Interest(
                id = "gastronomy",
                title = "Гастрономия",
                icon = android.R.drawable.ic_menu_agenda // Замените на R.drawable.ic_gastronomy
            ),
            Interest(
                id = "history",
                title = "История",
                icon = android.R.drawable.ic_menu_recent_history // Замените на R.drawable.ic_history
            ),
            Interest(
                id = "music",
                title = "Музыка",
                icon = android.R.drawable.ic_lock_silent_mode_off // Замените на R.drawable.ic_music
            ),
            Interest(
                id = "forest_hiking",
                title = "Лес и походы",
                icon = android.R.drawable.ic_menu_mylocation // Замените на R.drawable.ic_forest
            )
        )
    }
}