package com.example.aigpsradio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.aigpsradio.R


@Composable
fun AudioPlayerSheet(
    uiState: com.example.aigpsradio.viewmodel.LocationAudioUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Название места
        Text(
            //text = uiState.currentPlaceName ?: "Поиск места...",
            text = "Плотина городского пруда на реке Исеть",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Текущий трек
        Text(
            text = uiState.currentTrackName ?: "Трек не воспроизводится",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Индикатор загрузки
        if (uiState.isLoadingPlace || uiState.isLoadingAudio) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isLoadingPlace) "Определение места..." else "Загрузка аудио...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Сообщение об ошибке
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val shouldShowStartButton = !uiState.isPlaying &&
                !uiState.isPaused &&
                uiState.queueSize > 0 &&
                !uiState.hasStartedManually

        if (shouldShowStartButton) {
            // Показываем только кнопку "Начать воспроизведение"
            Button(
                onClick = onPlayPause,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Начать воспроизведение")
            }
        } else {
            // Показываем кнопки плеера только когда воспроизведение началось
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка Previous
                IconButton(
                    onClick = onSkipPrevious,
                    enabled = uiState.currentTrackIndex > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_previous),
                        contentDescription = "Previous",
                        tint = if (uiState.currentTrackIndex > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause кнопка
                IconButton(
                    onClick = onPlayPause,
                    enabled = uiState.queueSize > 0,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),

                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    // сама иконка внутри кнопки (размер иконки меньше, чем кнопки)
                    Icon(
                        painter = painterResource(
                            id = if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (uiState.isPlaying) "Пауза" else "Воспроизвести",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Кнопка Next
                IconButton(
                    onClick = onSkipNext,
                    enabled = uiState.queueSize > uiState.currentTrackIndex + 1,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_next),
                        contentDescription = "Skip",
                        tint = if (uiState.queueSize > uiState.currentTrackIndex + 1) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Индикатор статуса воспроизведения
        if (uiState.queueSize == 0 && !uiState.isLoadingPlace) {
            Text(
                text = "Нет доступных треков",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}