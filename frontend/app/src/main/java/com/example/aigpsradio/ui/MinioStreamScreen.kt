package com.example.aigpsradio.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.aigpsradio.data.repository.Repository
import com.example.aigpsradio.di.NetworkModule
import com.example.aigpsradio.viewmodel.AudioStreamViewModel
import com.example.aigpsradio.viewmodel.ViewModelFactory

@OptIn(UnstableApi::class)
@Composable
fun MinioStreamScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Создание Repository и ViewModel через DI
    val viewModel: AudioStreamViewModel = viewModel(
        factory = ViewModelFactory(
            Repository(NetworkModule.api)
        )
    )

    // Наблюдение за состоянием
    val uiState by viewModel.uiState.collectAsState()

    // ExoPlayer instance
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    // Состояние для отслеживания воспроизведения
    var isPlaying by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) } // свернут/развернут
    val height by animateDpAsState(if (expanded) 300.dp else 80.dp) // анимируем высоту

    // Управление lifecycle плеера
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                player?.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    // Создание/обновление плеера при изменении файла
    LaunchedEffect(uiState.audioFile) {
        if (uiState.audioFile != null && uiState.isPlaying) {
            player?.release()

            val newPlayer = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(uiState.audioFile!!.toURI().toString())

            newPlayer.setMediaItem(mediaItem)
            newPlayer.prepare()
            newPlayer.playWhenReady = true

            player = newPlayer
            isPlaying = true
        } else if (!uiState.isPlaying) {
            player?.release()
            player = null
            isPlaying = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка для выдвижения/сворачивания
            Button(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Свернуть" else "Развернуть")
            }

            // Контент виден только в развернутом виде
            if (expanded) {
                Text("ИИ-радио для путешественников онлайн", maxLines = 1)

        Button(
            onClick = { viewModel.loadAudio("plotinka_part1.m4a", context) },
            enabled = !uiState.isLoading
        ) {
            Text("Начать воспроизведение трека")
        }

        // Play / Pause кнопка
        player?.let { exoPlayer ->
            Button(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                }
            ) {
                Text(if (isPlaying) "Пауза" else "Продолжить")
            }
        }
                // Loading indicator
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}