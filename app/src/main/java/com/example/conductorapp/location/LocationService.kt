package com.example.conductorapp.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.conductorapp.ConHome
import com.example.conductorapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : LifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var locationCallback: LocationCallback



    override fun onCreate() {
        super.onCreate()

        firestore = FirebaseFirestore.getInstance()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationService()
            ACTION_STOP -> stopLocationService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationService() {
        Log.i("my_tag","Service Started")
        val channelId = "LocationServiceChannel"
        val notificationIntent = Intent(this, ConHome::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Fetching location...")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Service Channel", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        startForeground(1, notification)

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
        }
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationCallback(){
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val location = locationResult.lastLocation
                updateNotification(location)
                createOrUpdateDocumentWithLocation(location!!)
            }
        }
    }
    private fun updateNotification(location: Location?) {
        val channelId = "LocationServiceChannel"
        val notificationIntent = Intent(this, ConHome::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val contentText = location?.let { "Latitude: ${it.latitude}, Longitude: ${it.longitude}" } ?: "Location not available"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }


    private fun stopLocationService() {
        Log.i("my_tag","Service Stopped")
        stopForeground(true)
        locationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }


    private fun createOrUpdateDocumentWithLocation(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            val documentId = "your_document_id_here" // Replace with your desired document ID

            // Create a map with the initial data
            val initialData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to location.accuracy,
                // Add other location properties as needed
            )

            firestore.collection("userLocations")
                .document(documentId)
                .set(initialData)
                .addOnSuccessListener {
                    updateDocumentWithAdditionalData(documentId)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }

    private fun updateDocumentWithAdditionalData(documentId: String) {

        val updateData = mapOf(
            "status" to "active"
        )

        // Update the document in Firestore
        firestore.collection("userLocations")
            .document(documentId)
            .update(updateData)
            .addOnSuccessListener {
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun Location.toMap(): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
        )
    }


    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
