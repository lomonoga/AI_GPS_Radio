package com.example.aigpsradio.ui

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@ExperimentalMaterial3Api
@Composable
fun PlayerScreen(viewModel: LocationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    // Слой на карте, который отображает местоположение

    // Автоматический запуск отслеживания при создании экрана
    LaunchedEffect(Unit) {
        viewModel.startLocationTracking()
        Log.d(TAG, "PlayerScreen created, starting location tracking")
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
        sheetPeekHeight = 120.dp,
        sheetContent = {
            MinioStreamScreen()
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

            // --- FAB для центрирования карты ---
            FloatingActionButton(
                onClick = {
                    locationOverlay?.myLocation?.let { loc ->
                        mapView?.controller?.animateTo(loc)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // правый нижний угол
                    .padding(end = 20.dp, bottom = 140.dp) // отступы от краёв
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