package com.example.aigpsradio.navigation

sealed class Destination (val route: String) {

    data object Permission : Destination(ROUTE_PERMISSION)
    data object VoiceInterests : Destination(ROUTE_VOICE_INTERESTS)
    data object InterestsSelection : Destination(ROUTE_INTERESTS_SELECTION)
    data object Player : Destination(ROUTE_PLAYER)

    companion object {
        private const val ROUTE_PERMISSION = "route_permission"
        private const val ROUTE_VOICE_INTERESTS = "route_voice_interests"
        private const val ROUTE_INTERESTS_SELECTION = "route_interests_selection"
        private const val ROUTE_PLAYER = "route_player"
    }
}