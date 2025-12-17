package com.example.aigpsradio.ui

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aigpsradio.R
import com.example.aigpsradio.viewmodel.LocationAudioViewModel
import com.example.aigpsradio.viewmodel.LocationViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    locationviewModel: LocationViewModel,
    locationAudioViewModel: LocationAudioViewModel,
    onOpenInterests: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by locationAudioViewModel.uiState.collectAsState()


    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var hasStartedPlayback by remember { mutableStateOf(false) }
    var poiMarker by remember { mutableStateOf<Marker?>(null) }

    // Автоматический запуск отслеживания при создании экрана
    LaunchedEffect(Unit) {
        locationviewModel.startLocationTracking()
        Log.d(TAG, "PlayerScreen created, starting location tracking")
    }

    LaunchedEffect(locationOverlay) {
        snapshotFlow { locationOverlay?.myLocation }
            .collect { location ->
                location?.let {
                    val lat = it.latitude
                    val lon = it.longitude

                    // Обновляем локацию в locationaudioViewModel
                    locationAudioViewModel.updateLocation(lat, lon)

                    // Запускаем воспроизведение при получении первой локации
                    if (!hasStartedPlayback) {
                        locationAudioViewModel.startLocationBasedPlayback(lat, lon)
                        hasStartedPlayback = true
                        Log.d(TAG, "Started location-based playback at $lat, $lon")
                    }
                }
            }
    }
    // Добавьте LaunchedEffect для отслеживания изменений места
    LaunchedEffect(uiState.currentPlaceName) {
        // Получаем координаты текущего места из API
        val currentPlace = locationAudioViewModel.getCurrentPlaceCoordinates()

        currentPlace?.let { (lat, lon) ->
            mapView?.let { map ->
                // Удаляем старый маркер если есть
                poiMarker?.let { map.overlays.remove(it) }

                // Создаем новый маркер
                val marker = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = uiState.currentPlaceName ?: "POI"
                    icon = ContextCompat.getDrawable(
                        map.context,
                        R.drawable.ic_place // используйте свою иконку
                    )
                }

                map.overlays.add(marker)
                poiMarker = marker
                map.invalidate()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {  // при уничтожении экрана
            lifecycleOwner.lifecycle.removeObserver(observer) // отписываемся
            locationOverlay?.disableMyLocation() // выключаем отслеживание
            mapView?.overlays?.clear() // очищаем все слои карты
            mapView?.onDetach() // отключаем карту
            Log.d(TAG, "PlayerScreen disposed, map cleaned up")
        }
    }

    // --- Bottom sheet state (starts partially expanded) ---
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 150.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {

            val configuration = LocalConfiguration.current
            val sheetHeight = (configuration.screenHeightDp * 0.75f).dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight) // задаём максимальную высоту листа — 75% экрана
            ) {

                val currentState = scaffoldState.bottomSheetState.currentValue

                AnimatedVisibility(
                    visible = currentState == SheetValue.Expanded,
                    enter = fadeIn() + expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                            shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 200)
                            )
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PlaceImage( viewModel = locationAudioViewModel)
//                            Image(
//                                painter = painterResource(id = R.drawable.plotinka),
//                                contentDescription = "My Location",
//                                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
//                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AudioPlayerSheet(
                        uiState = uiState,
                        onPlayPause = {
                            if (uiState.isPlaying) {
                                locationAudioViewModel.pausePlayback()
                            } else {
                                locationAudioViewModel.resumePlayback()
                            }
                        },
                        onSkipNext = { locationAudioViewModel.skipToNext() },
                        onSkipPrevious = { locationAudioViewModel.skipToPrevious() }
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- Map in background ---
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(56.837502, 60.608574))
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0

                        val overlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx),
                            this
                        )
                        overlay.enableMyLocation()
                        overlay.enableFollowLocation()
                        overlays.add(overlay)

                        locationOverlay = overlay
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .align(Alignment.CenterEnd) // или Alignment.TopEnd, если хочешь прижать к верху
                    .padding(end = 16.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween, // распределит одну FAB сверху, другую снизу
                horizontalAlignment = Alignment.End
            ) {

                // --- FAB для открытия экрана интересов---
                FloatingActionButton(
                    onClick = { onOpenInterests() }, // вызываем callback
                    modifier = Modifier
                        .padding(end = 10.dp, top = 40.dp)
                        .size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_interests),
                        contentDescription = "Interests"
                    )
                }

                // --- FAB для центрирования карты ---
                FloatingActionButton(
                    onClick = {
                        locationOverlay?.myLocation?.let { loc ->
                            mapView?.controller?.animateTo(loc)
                        }
                    },
                    modifier = Modifier
                        .padding(end = 10.dp, bottom = 180.dp)
                        .size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_my_location),
                        contentDescription = "My Location"
                    )
                }
            }
        }
    }
}