package com.example.aigpsradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.aigpsradio.R

@Composable
fun VoiceInterestsScreen(
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBarSimple(title = "Расскажите о себе")
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),

                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Пропустить")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Подтвердить ввод")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Опишите голосом свои интересы и предпочтения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Микрофон кнопка
            Card(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.mic),
                                contentDescription = "Микрофон",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Готово!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Текстовое поле с примером
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Мне нравятся горы, исторические места и местная кухня. Я люблю фотографировать природу и изучать культуру новых мест.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
@Preview(showBackground = true)
fun VoiceInterestsScreenPrewiew() {
    VoiceInterestsScreen()
}