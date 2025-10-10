package com.example.aigpsradio.ui

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.example.aigpsradio.ui.theme.Typography
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aigpsradio.R
import com.example.aigpsradio.ui.theme.MyApplicationTheme

@Composable
fun PermissionItem(
    icon: Int,
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit,
    showGrantedAsText: Boolean = false // новый параметр
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (granted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Иконка слева
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Текст справа от иконки
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (granted && showGrantedAsText) {
                // ✅ Вместо кнопки показываем надпись с галочкой
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Разрешение предоставлено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(if (granted) "Разрешено" else "Разрешить")
                }
            }
        }
    }
}

@Composable
fun PermissionsScreenSimple(
    initialLocationGranted: Boolean = false,
    initialBackgroundGranted: Boolean = false,
    initialMicGranted: Boolean = false,
    initialNotifsGranted: Boolean = false
) {
    var locationGranted by remember { mutableStateOf(initialLocationGranted) }
    var backgroundGranted by remember { mutableStateOf(initialBackgroundGranted) }
    var micGranted by remember { mutableStateOf(initialMicGranted) }
    var notifsGranted by remember { mutableStateOf(initialNotifsGranted) }

    val screenPadding = 16.dp
    val bottomBarHeight = 72.dp

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBarSimple(title = "Разрешения приложения")
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPadding, vertical = 12.dp)
                        .height(bottomBarHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Продолжить")
                    }
                }
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = screenPadding)
                    .fillMaxSize()
            ) {

                Text(
                    text = "Для работы приложения необходимы следующие разрешения",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Геолокация — показываем текст вместо кнопки, если разрешено
                PermissionItem(
                    icon = R.drawable.ic_location,
                    title = "Доступ к геолокации",
                    subtitle = "Для предоставления контента, основанного на вашем местоположении",
                    granted = locationGranted,
                    onAction = { locationGranted = !locationGranted },
                    showGrantedAsText = true
                )

                // Фоновая геолокация
                PermissionItem(
                    icon = R.drawable.ic_location,
                    title = "Фоновая геолокация",
                    subtitle = "Для воспроизведения радио и отслеживания в фоне",
                    granted = backgroundGranted,
                    onAction = { backgroundGranted = !backgroundGranted }
                )

                // Микрофон
                PermissionItem(
                    icon = R.drawable.mic,
                    title = "Доступ к микрофону",
                    subtitle = "Для голосового управления и выбора интересов",
                    granted = micGranted,
                    onAction = { micGranted = !micGranted }
                )

                // Уведомления
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        icon = R.drawable.notifications,
                        title = "Уведомления",
                        subtitle = "Разрешение на показ уведомлений для управления воспроизведением",
                        granted = notifsGranted,
                        onAction = { notifsGranted = !notifsGranted }
                    )
                } else {
                    PermissionItem(
                        icon = R.drawable.notifications,
                        title = "Уведомления (demo)",
                        subtitle = "Уведомления доступны на вашей версии",
                        granted = notifsGranted,
                        onAction = { notifsGranted = !notifsGranted }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSimple(title: String) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}

@Preview(
    showBackground = true,
    device = "id:pixel_6",
    name = "Pixel 6 API 33",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun PreviewPermissionsScreenDark() {
    MyApplicationTheme(darkTheme = true) {
        PermissionsScreenSimple(
            initialLocationGranted = true
        )
    }
}

@Preview(
    showBackground = true,
    device = "id:pixel_6",
    name = "Pixel 6 Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PreviewPermissionsScreenLight() {
    MyApplicationTheme(darkTheme = false) {
        PermissionsScreenSimple(
            initialLocationGranted = true
        )
    }
}