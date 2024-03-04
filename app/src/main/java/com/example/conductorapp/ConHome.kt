package com.example.conductorapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import com.example.conductorapp.location.LocationService
import com.google.firebase.auth.FirebaseAuth

class ConHome : AppCompatActivity() {
    private lateinit var btnRideStart: Button
    private lateinit var btnRideStop: Button
    private lateinit var btnSignOut: Button
    private lateinit var btnScanTicket: Button
    private lateinit var btnMap: Button
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_con_home)

        init()


        btnRideStart.setOnClickListener {
            val intent = Intent(this, LocationService::class.java)
            intent.action = LocationService.ACTION_START
            startService(intent)
        }

        btnRideStop.setOnClickListener {
            val intent = Intent(this, LocationService::class.java)
            intent.action = LocationService.ACTION_STOP
            startService(intent)
        }

        btnMap.setOnClickListener {
            val intent = Intent(this, Map::class.java)
            startActivity(intent)
        }

        btnScanTicket.setOnClickListener {
            val intent = Intent(this, ScanTicket::class.java)
            startActivity(intent)
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, ConLogin::class.java))
        }

    }

    private fun init(){
        btnRideStart = findViewById(R.id.btnRideStart_conHome)
        btnRideStop = findViewById(R.id.btnRideStop_conHome)
        btnMap = findViewById(R.id.btnMap_conHome)
        btnSignOut = findViewById(R.id.btnSignOut_conHome)
        btnScanTicket = findViewById(R.id.btnScanTicket_conHome)
        auth = FirebaseAuth.getInstance()
    }
}