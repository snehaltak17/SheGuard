package com.sheguard.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.sheguard.dashboard.DashboardActivity
import com.sheguard.databinding.ActivityLoginBinding
import com.sheguard.firebase.FirebaseDebugUtils

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        FirebaseDebugUtils.logFirebaseConfiguration()

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        binding.registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!FirebaseDebugUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection available.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Attempting login for email=$email")

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Login success for uid=${firebaseAuth.currentUser?.uid}")
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Login failed", exception)
                Toast.makeText(
                    this,
                    FirebaseDebugUtils.authErrorMessage(exception),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            Log.d(TAG, "Existing session found for uid=${firebaseAuth.currentUser?.uid}")
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
