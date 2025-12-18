package com.example.aigpsradio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aigpsradio.R
import com.example.aigpsradio.viewmodel.LocationAudioUiState


@Composable
fun AudioPlayerSheet(
    uiState: LocationAudioUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Название места
        Text(
            text = uiState.currentPlaceName ?: "Загрузка места",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                Text(
                    text ="Начать воспроизведение",
                    style = MaterialTheme.typography.bodyMedium
                    )
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
                    enabled = uiState.queueSize > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_previous),
                        contentDescription = "Previous",
                        tint = if (uiState.queueSize > 0) {
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

        // Текущий трек
        Text(
            text = uiState.currentTrackName ?: "Трек не воспроизводится",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Только индикатор загрузки аудио
        // if (uiState.isLoadingPlace || uiState.isLoadingAudio) {
        if (uiState.isLoadingAudio) {
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
                    text = "Загрузка аудио...",
                    // text = if (uiState.isLoadingPlace) "Определение места..." else "Загрузка аудио...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Сообщение об ошибке
        uiState.errorMessage?.let { error ->
            Text(
                // text = error,
                text = "Что-то пошло не так, проверьте подключение к интернету",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Описание места
        Text(
            text = uiState.currentPlaceDescription ?: "Загрузка описания",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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