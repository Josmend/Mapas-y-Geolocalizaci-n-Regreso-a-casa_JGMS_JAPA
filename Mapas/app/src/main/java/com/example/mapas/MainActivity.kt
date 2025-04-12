package com.example.mapas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapas.ui.theme.MapasTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiService = RouteApiService.create()
    private val LOCATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Configuration.getInstance().load(this, getPreferences(Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MapasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MapScreen()
                }
            }
        }
    }

    @Composable
    fun MapScreen() {
        val context = LocalContext.current
        var routePoints by remember { mutableStateOf<List<GeoPoint>?>(null) }
        var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var street by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }

        var hasLocationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }


        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasLocationPermission = isGranted
            if (!isGranted) {

            }
        }


        fun getCurrentLocation() {
            if (!hasLocationPermission) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        return@addOnSuccessListener
                    }
                    currentLocation = GeoPoint(location.latitude, location.longitude)
                }
                .addOnFailureListener {

                }
        }


        fun calculateRoute() {
            if (currentLocation == null || street.isEmpty() || number.isEmpty() || city.isEmpty()) {

                return
            }

            isLoading = true

            val address = "$street $number, $city"


            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val geocodeResponse = apiService.geocodeAddress(
                        apiKey = RouteApiService.API_KEY,
                        address = address
                    )

                    val destination = geocodeResponse.features.firstOrNull()?.geometry?.coordinates
                    if (destination != null) {

                        val endLat = destination[1]
                        val endLon = destination[0]


                        val startLat = currentLocation!!.latitude
                        val startLon = currentLocation!!.longitude
                        val start = "$startLon,$startLat"
                        val end = "$endLon,$endLat"

                        val route = apiService.getRoute(
                            apiKey = RouteApiService.API_KEY,
                            start = start,
                            end = end
                        )

                        val points = route.features.firstOrNull()?.geometry?.coordinates?.map {
                            GeoPoint(it[1], it[0])
                        } ?: emptyList()

                        withContext(Dispatchers.Main) {
                            routePoints = points
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }


        Scaffold(
            floatingActionButton = {

                FloatingActionButton(onClick = {
                    getCurrentLocation()
                    calculateRoute()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Aceptar")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                MapViewComponent(
                    currentLocation = currentLocation,
                    routePoints = routePoints
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }


        Column(modifier = Modifier.padding(16.dp)) {
            TextField(value = street, onValueChange = { street = it }, label = { Text("Calle") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = number, onValueChange = { number = it }, label = { Text("Número") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = city, onValueChange = { city = it }, label = { Text("Ciudad") })
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculateRoute() }) {
                Text("Aceptar")
            }
        }
    }

    @Composable
    fun MapViewComponent(
        currentLocation: GeoPoint?,
        routePoints: List<GeoPoint>?
    ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    setMultiTouchControls(true)
                }
            },
            update = { mapView ->
                currentLocation?.let { location ->
                    mapView.controller.setCenter(location)
                    mapView.controller.setZoom(15.0)
                }


                mapView.overlays.clear()


                currentLocation?.let { location ->
                    val marker = Marker(mapView)
                    marker.position = location
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Tu ubicación"
                    mapView.overlays.add(marker)
                }


                routePoints?.let { points ->
                    val line = Polyline()
                    line.setPoints(points)
                    line.color = android.graphics.Color.BLUE
                    mapView.overlayManager.add(line)
                }

                mapView.invalidate()
            }
        )
    }
}
