package com.example.conductorapp

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Map : AppCompatActivity(), OnMapReadyCallback,
    Callback<DirectionsResponse?> {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var fabUserLocation: View
    private lateinit var fabLocationSearch: View
    private lateinit var btnDisplayRoute: Button
    private lateinit var tvDistance: TextView
    private lateinit var tvS: TextView
    private lateinit var tvD: TextView
    private lateinit var home: CarmenFeature
    private var userLocationMarker: Marker? = null
    private lateinit var work: CarmenFeature
    private lateinit var stopId: MutableList<String>
    private lateinit var stops: MutableList<Point>
    private lateinit var stop: MutableList<Point>
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val busPosIconId = "busPosIconId"
    private var origin: Point = Point.fromLngLat(90.399452, 23.777176)
    private var destination: Point = Point.fromLngLat(90.399452, 23.777176)
    private var client: MapboxDirections? = null
    private var c = 0
    private var distance = 0.0
    private var previousLocation: LatLng? = null
    private var currentLocation: LatLng? = null
    private var marker: Marker? = null
    private var currentLoc: LatLng? = null
    private var st: String? = null
    private var startLocation: String? = ""
    private var endLocation: String? = ""


    /**
     * locationCallback get new device location and returns a
     * callback , assigning the new location to currentLoc
    */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->

                currentLoc = LatLng(location.latitude, location.longitude)
                val ic = IconFactory.getInstance(this@Map).fromBitmap(BitmapUtils.getBitmapFromDrawable(ResourcesCompat.getDrawable(resources, R.drawable.bus_position_symbol, null))!!)

                if(marker == null) {
                    marker = mapboxMap.addMarker(
                        MarkerOptions()
                            .position(currentLoc)
                            .icon(ic)

                    )
                }

                updateMarkerPositionWithAnimation(currentLoc!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            resources.getString(R.string.accessToken)
        )
        setContentView(R.layout.activity_map)
        init()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun updateMarkerPositionWithAnimation(newLocation: LatLng) {
        Log.i("my_tag", "marker update fun called")

        previousLocation = currentLocation
        currentLocation = newLocation
        Log.i("my_tag", "previous location: $previousLocation")
        Log.i("my_tag", "current location: $currentLocation")

        //Add marker at the new location with smooth animation
        previousLocation?.let { prevLocation ->
            val interpolator = LinearInterpolator()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 1000 // Animation duration in milliseconds
            valueAnimator.addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val lat = (currentLocation!!.latitude - prevLocation.latitude) * fraction + prevLocation.latitude
                val lng = (currentLocation!!.longitude - prevLocation.longitude) * fraction + prevLocation.longitude
                val animatedPosition = LatLng(lat, lng)
                marker?.position = animatedPosition
                mapboxMap.updateMarker(marker!!)
            }
            valueAnimator.interpolator = interpolator
            valueAnimator.start()
        }
    }

    private fun init(){
        mapView = findViewById<View>(R.id.mapView) as MapView
        fabUserLocation = findViewById(R.id.fabUserLocation)
        fabLocationSearch = findViewById(R.id.fabLocationSearch)
        tvDistance = findViewById(R.id.distanceView)
        tvS = findViewById(R.id.tvS)
        tvD = findViewById(R.id.tvD)
        btnDisplayRoute = findViewById(R.id.btnDisplayRoute)
        db = FirebaseFirestore.getInstance()
        stopId = mutableListOf()
        stops = mutableListOf()
    }


    /**
     * onMapReady is a callback invoked when the map is ready to use
     * it is invoked once map is initialized and style is loaded
     */
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) { style ->
            startLocationUpdates()
            initSearchFab()
            moveToCurrentLoc()
            setupBusPosIcon(style)
            setUpSource(style)
            setupLayer(style)
        }
    }

    private fun initSearchFab() {
        fabLocationSearch.setOnClickListener {
            val intent: Intent = PlaceAutocomplete.IntentBuilder()
                .placeOptions(
                    PlaceOptions.builder()
                        .limit(10)
                        .build(PlaceOptions.MODE_CARDS)
                )
                .accessToken(resources.getString(R.string.accessToken))
                .build(this@Map)

            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }

    private fun moveToCurrentLoc() {
        fabUserLocation.setOnClickListener {
            currentLoc?.let {
                val position: CameraPosition = CameraPosition.Builder()
                    .target(currentLoc)
                    .zoom(14.0)
                    .tilt(13.0)
                    .build()

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)

            }
        }
    }

    private fun setUpSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(geojsonSourceLayerId))
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                PropertyFactory.iconImage(busPosIconId),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    private fun setupBusPosIcon(style: Style){
        val bit = BitmapUtils.getBitmapFromDrawable(ResourcesCompat
            .getDrawable(resources,
                R.drawable.bus_position_symbol,
                null)
        )

        style.addImage(busPosIconId, bit!!)

    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(this,
                        "Location permission denied. Cannot show current location.",
                        Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            val selectedCarmenFeature: CarmenFeature = PlaceAutocomplete.getPlace(data)
            val style = mapboxMap.style
            if (style != null) {
//                val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
//                source?.setGeoJson(
//                    FeatureCollection.fromFeatures(
//                        arrayOf(
//                            Feature.fromJson(
//                                selectedCarmenFeature.toJson()
//                            )
//                        )
//                    )
//                )
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    (selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                                    (selectedCarmenFeature.geometry() as Point?)!!.longitude()
                                )
                            )
                            .zoom(14.0)
                            .build()
                    ), 4000
                )
            }
        }
    }

    override fun onResponse(
        call: Call<DirectionsResponse?>,
        response: Response<DirectionsResponse?>
    ) {
        if (response.body() == null) {
            Toast.makeText(
                this@Map,
                "No routes found, make sure to set right user and access token",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this@Map, "NO routes found", Toast.LENGTH_LONG).show()
            return
        }
    }

    override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {}

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
        private const val PERMISSIONS_REQUEST_LOCATION = 123
    }
}

