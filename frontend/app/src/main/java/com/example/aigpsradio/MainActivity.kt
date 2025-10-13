package com.example.aigpsradio

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.aigpsradio.navigation.AppNavHost
import com.example.aigpsradio.ui.InterestsSelectionScreen
import com.example.aigpsradio.ui.PermissionsScreenSimple
import com.example.aigpsradio.ui.VoiceInterestsScreen
import com.example.aigpsradio.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {

                val navController = rememberNavController()
                AppNavHost(navHostController = navController)

                // Для демо - версия с кнопками навигации
                // AppScreenPreview()

                // Для продакшна - обычный flow
                // AppNavigationFlow()
            }
        }
    }
}

@Composable
fun AppNavigationFlow() {
    // Измените здесь стартовый экран для демо:
    // Screen.Permissions - экран разрешений
    // Screen.InterestsSelection - экран выбора интересов
    // Screen.VoiceInterests - экран голосового ввода
    var currentScreen by remember { mutableStateOf(Screen.InterestsSelection) }

    when (currentScreen) {
        Screen.Permissions -> {
            PermissionsScreenSimple(
                initialLocationGranted = true,
                initialBackgroundGranted = false,
                initialMicGranted = false,
                initialNotifsGranted = false,
                onContinue = TODO()
            )
            // Для перехода на следующий экран можно добавить onContinue callback
            // В данный момент переход ручной через кнопки
        }

        Screen.InterestsSelection -> {
            InterestsSelectionScreen(
                onContinue = {
                    currentScreen = Screen.VoiceInterests
                }
            )
        }

        Screen.VoiceInterests -> {
            VoiceInterestsScreen(
                onComplete = {
                    // Переход на главный экран приложения
                    currentScreen = Screen.MainApp
                }
            )
        }

        Screen.MainApp -> {
            // Заглушка для главного экрана
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Главный экран приложения")
                }
            }
        }
    }
}

enum class Screen {
    Permissions,
    InterestsSelection,
    VoiceInterests,
    MainApp
}

// Для быстрого превью разных экранов
@Composable
fun AppScreenPreview(startScreen: Screen = Screen.Permissions) {
    MyApplicationTheme {
        var currentScreen by remember { mutableStateOf(startScreen) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Навигационные кнопки для превью
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { currentScreen = Screen.Permissions }) {
                    Text("1")
                }
                Button(onClick = { currentScreen = Screen.InterestsSelection }) {
                    Text("2")
                }
                Button(onClick = { currentScreen = Screen.VoiceInterests }) {
                    Text("3")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    Screen.Permissions -> PermissionsScreenSimple(
                        initialLocationGranted = true,
                        initialBackgroundGranted = TODO(),
                        initialMicGranted = TODO(),
                        initialNotifsGranted = TODO(),
                        onContinue = TODO()
                    )
                    Screen.InterestsSelection -> InterestsSelectionScreen(
                        onContinue = { currentScreen = Screen.VoiceInterests }
                    )
                    Screen.VoiceInterests -> VoiceInterestsScreen(
                        onComplete = { currentScreen = Screen.MainApp }
                    )
                    Screen.MainApp -> {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Главный экран")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppScreenPreview(startScreen = Screen.InterestsSelection)
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun PreviewPermissionsScreenDark() {
    MyApplicationTheme(darkTheme = true) {
        PermissionsScreenSimple(
            initialLocationGranted = true,
            initialBackgroundGranted = TODO(),
            initialMicGranted = TODO(),
            initialNotifsGranted = TODO(),
            onContinue = TODO()
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PreviewPermissionsScreenLight() {
    MyApplicationTheme(darkTheme = false) {
        PermissionsScreenSimple(
            initialLocationGranted = true,
            initialBackgroundGranted = TODO(),
            initialMicGranted = TODO(),
            initialNotifsGranted = TODO(),
            onContinue = TODO()
        )
    }
}
