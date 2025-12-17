package com.example.aigpsradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigpsradio.R
import com.example.aigpsradio.model.Interest
import com.example.aigpsradio.ui.theme.MyApplicationTheme

/**
 * Карточки с иконками: Развелечения, Природа, Архитектура, Гастрономия
 * Кнопка "Продолжить" активна только если выбран хотя бы один интерес
 */

@Composable
fun InterestCategoryCard(
    interest: Interest,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Card(
        onClick = onToggle,
        modifier = modifier
            .aspectRatio(1f) // задаём соотношение сторон 1:1 (квадрат)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = interest.icon),
                        contentDescription = interest.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp)) // маленький горизонтальный отступ после галочки
                    }
                    Text(
                        text = interest.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    onOpenVoiceInterests: () -> Unit = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )

                FloatingActionButton(
                    onClick = { onOpenVoiceInterests() },
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_voice),
                        contentDescription = "Action"
                    )
                }
            }
        }
    )
}

@Composable
fun InterestsSelectionScreen(
    onContinue: () -> Unit = {},
    onOpenVoiceInterests: () -> Unit = {},
) {
    val interests = remember {
        listOf(
            Interest("architecture", "Архитектура", R.drawable.ic_architecture),
            Interest("gastronomy", "Гастрономия", R.drawable.ic_food),
            Interest("history", "История", R.drawable.ic_history),
            Interest("nature", "Природа", R.drawable.ic_nature)
        )
    }

    var selectedInterests by remember {
        mutableStateOf(setOf("architecture")) // Архитектура выбрана по умолчанию
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CustomTopAppBar(title = "Ваши интересы", onOpenVoiceInterests =  onOpenVoiceInterests)
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = selectedInterests.isNotEmpty()
                    ) {
                        Text("Продолжить")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 15.dp
                )
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Выберите темы, которые вас интересуют",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Сетка 2x3
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                interests.chunked(2).forEach { rowInterests ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowInterests.forEach { interest ->
                            InterestCategoryCard(
                                interest = interest,
                                isSelected = interest.id in selectedInterests,
                                onToggle = {
                                    selectedInterests = if (interest.id in selectedInterests) {
                                        selectedInterests - interest.id
                                    } else {
                                        selectedInterests + interest.id
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Превью экрана (светлая тема)
@Composable
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
)
fun PreviewInterestsSelectionScreen_Light() {
    MaterialTheme {
        Surface {
            InterestsSelectionScreen()
        }
    }
}

// Превью экрана (темная тема)
@Composable
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
)
fun PreviewInterestsSelectionScreen_Dark() {
    MaterialTheme {
        MyApplicationTheme(darkTheme = true) {
            InterestsSelectionScreen(onContinue = {})
        }
    }
}

