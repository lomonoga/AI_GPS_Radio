package com.example.aigpsradio.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.aigpsradio.R
import com.example.aigpsradio.data.repository.Repository
import com.example.aigpsradio.di.NetworkModule
import com.example.aigpsradio.viewmodel.AudioStreamViewModel
import com.example.aigpsradio.viewmodel.ViewModelFactory

@OptIn(UnstableApi::class)
@Composable
fun MinioStreamScreen( onPlay: () -> Unit = {}) {
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
            .heightIn(min = 160.dp, max = 300.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Контент виден только в развернутом виде
            Text("Плотина Городского пруда на реке Исеть", maxLines = 1)

            Button(
                onClick = {
                    viewModel.loadAudio("plotinka_part1.m4a", context)
                    onPlay()
                    },
                enabled = !uiState.isLoading
            ) {
                Text("Начать воспроизведение трека")
            }

            // Play / Pause кнопка
            player?.let { exoPlayer ->
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },

                    // размер внешней круглой кнопки
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),

                    // настраиваем фон кнопки через цвета (material3)
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    // сама иконка внутри кнопки (размер иконки меньше, чем кнопки)
                    Icon(
                        painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
        }
    }
}
