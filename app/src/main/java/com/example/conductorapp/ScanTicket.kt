package com.example.conductorapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator

class ScanTicket : AppCompatActivity() {
    private lateinit var scannedData: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_ticket)

        // Initialize QR code scanner
        val integrator = IntentIntegrator(this)
        integrator.setCameraId(0)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    // Handle the result of QR code scanning
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i("my_tag","OnActivityResult")


        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            scannedData = result.contents
            compareWithFirestoreData(scannedData)
        } else {
            // Handle case when no QR code was scanned
            // You may want to show a message or take appropriate action
        }
    }

    private fun compareWithFirestoreData(scannedData: String) {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("validTickets").whereEqualTo("ticketId", scannedData)

        docRef.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {

                    Toast.makeText(this, "VALID TICKET", Toast.LENGTH_LONG).show()

                } else {
                    Toast.makeText(this, "INVALID TICKET", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->

            }
    }
}
