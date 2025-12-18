package com.example.aigpsradio.model

import com.example.aigpsradio.R

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
                icon = R.drawable.ic_nature
            ),
            Interest(
                id = "architecture",
                title = "Архитектура",
                icon = R.drawable.ic_architecture
            ),
            Interest(
                id = "gastronomy",
                title = "Гастрономия",
                icon = R.drawable.ic_food
            ),
            Interest(
                id = "history",
                title = "История",
                icon = R.drawable.ic_history
            )
        )
    }
}