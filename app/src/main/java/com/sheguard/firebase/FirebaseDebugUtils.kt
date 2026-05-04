package com.sheguard.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DatabaseError

object FirebaseDebugUtils {

    private const val TAG = "SheGuardFirebase"

    fun logFirebaseConfiguration() {
        val app = FirebaseApp.getInstance()
        val options = app.options
        Log.d(
            TAG,
            "Firebase config: projectId=${options.projectId}, appId=${options.applicationId}, databaseUrl=${options.databaseUrl}"
        )
        if (options.databaseUrl.isNullOrBlank()) {
            Log.e(
                TAG,
                "Realtime Database URL missing from FirebaseOptions. Create Realtime Database in Firebase console and download a fresh google-services.json."
            )
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
            else -> false
        }
    }

    fun authErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password format."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthInvalidUserException -> "No account found for this email."
            is FirebaseNetworkException -> "No internet connection. Please check the emulator or device network."
            is FirebaseAuthException -> when (exception.errorCode) {
                "CONFIGURATION_NOT_FOUND" -> "Firebase Authentication is not configured correctly. Enable Email/Password sign-in in Firebase console and verify google-services.json."
                "TOO_MANY_REQUESTS" -> "Too many attempts. Please wait and try again."
                else -> exception.localizedMessage ?: "Authentication failed."
            }
            else -> exception?.localizedMessage ?: "Something went wrong. Please try again."
        }
    }

    fun databaseErrorMessage(error: DatabaseError): String {
        return when (error.code) {
            DatabaseError.NETWORK_ERROR -> "Database network error. Check internet connectivity."
            DatabaseError.PERMISSION_DENIED -> "Database permission denied. Check Firebase Realtime Database rules."
            DatabaseError.DISCONNECTED -> "Database disconnected. Try again when internet is available."
            else -> error.message
        }
    }
}
