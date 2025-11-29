package com.example.aigpsradio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.aigpsradio.R
import com.example.aigpsradio.viewmodel.LocationAudioViewModel

@Composable
fun PlaceImage(viewModel: LocationAudioViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val bmp = uiState.placeImageBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = uiState.currentPlaceName ?: "Place image",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.yekaterinburg),
            contentDescription = "Placeholder",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop
        )
    }
}