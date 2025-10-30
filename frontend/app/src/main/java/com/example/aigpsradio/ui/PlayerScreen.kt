package com.example.aigpsradio.ui

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aigpsradio.R
import com.example.aigpsradio.viewmodel.LocationViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


@Composable
fun PlayerScreen(viewModel: LocationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) } // Переменная для хранения карты
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    // Слой на карте, который отображает местоположение

    // Автоматический запуск отслеживания при создании экрана
    LaunchedEffect(Unit) {
        viewModel.startLocationTracking()
        Log.d(TAG, "PlayerScreen created, starting location tracking")
        }

    // Управление жизненным циклом MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { mapView?.onResume() }
                Lifecycle.Event.ON_PAUSE -> { mapView?.onPause() }
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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Карта занимает ВЕСЬ экран
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Настройки зума
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(56.837502, 60.608574))
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0

                        // Добавляем overlay для отображения текущей позиции
                        val myLocationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx),
                            this
                        )

                        myLocationOverlay.enableMyLocation()
                        myLocationOverlay.enableFollowLocation()
                        overlays.add(myLocationOverlay)

                        locationOverlay = myLocationOverlay
                        mapView = this

                        Log.d(TAG, "MapView initialized")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Кнопки управления внизу
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    // Центрируем карту на текущей позиции
                    val overlay = locationOverlay
                    val mv = mapView
                    if (overlay != null && mv != null) {
                        overlay.myLocation?.let { loc ->
                            mv.controller.animateTo(loc)
                            Log.d(TAG, "Centered map on current location")
                        } ?: run {
                            Log.d(TAG, "Current location is null — cannot center")
                        }
                    }
            },
                modifier = Modifier.size(56.dp) // стандартный размер FAB
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_my_location),
                    contentDescription = "My Location"
                )
            }
        }
    }
}