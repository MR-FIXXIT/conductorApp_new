package com.example.conductorapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ConLogin : AppCompatActivity() {

    private lateinit var btnSignin: Button
    private lateinit var etUsername: EditText
    private lateinit var etPass: EditText
    private lateinit var auth: FirebaseAuth

    private fun init(){
        auth = FirebaseAuth.getInstance()
        btnSignin = findViewById(R.id.btnSignIn_conLogin)
        etUsername = findViewById(R.id.etUsername_conLogin)
        etPass = findViewById(R.id.etPassword_conLogin)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_con_login)

        init()

        btnSignin.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser != null){
            startActivity(Intent(this, ConHome::class.java))
        }
    }

    private fun signIn(){
        val email = etUsername.text.toString()
        val pass = etPass.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty()) {

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    startActivity(Intent(this, ConHome::class.java))
                } else {
                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Fill the fields to continue", Toast.LENGTH_SHORT).show()
        }
    }
}