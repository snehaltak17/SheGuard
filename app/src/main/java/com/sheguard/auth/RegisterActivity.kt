package com.sheguard.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sheguard.dashboard.DashboardActivity
import com.sheguard.databinding.ActivityRegisterBinding
import com.sheguard.firebase.FirebaseDebugUtils

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        FirebaseDebugUtils.logFirebaseConfiguration()

        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!FirebaseDebugUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection available.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Attempting signup for email=$email")

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    Log.e(TAG, "Signup succeeded but Firebase user is null")
                    Toast.makeText(this, "Account created, but user session is unavailable.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userData = mapOf(
                    "uid" to user.uid,
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "createdAt" to System.currentTimeMillis()
                )

                Log.d(TAG, "Auth signup success for uid=${user.uid}; writing profile to RTDB")

                firebaseDatabase.getReference("users")
                    .child(user.uid)
                    .setValue(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Realtime Database profile write succeeded for uid=${user.uid}")
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { databaseException ->
                        Log.e(TAG, "Realtime Database write failed", databaseException)
                        Toast.makeText(
                            this,
                            databaseException.localizedMessage
                                ?: "Failed to save your profile to the database.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Signup failed", exception)
                Toast.makeText(
                    this,
                    FirebaseDebugUtils.authErrorMessage(exception),
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
