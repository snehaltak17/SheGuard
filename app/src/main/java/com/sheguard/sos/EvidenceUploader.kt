package com.sheguard.sos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class EvidenceUploader(private val context: Context) {

    suspend fun uploadEvidence(imageUri: Uri?): String? {
        return try {
            val photoFile = imageUri?.let { uri ->
                File(uri.path ?: "")
            } ?: createPlaceholderEvidenceFile()
            uploadToFirebaseStorage(photoFile)
        } catch (exception: Exception) {
            Log.e("EvidenceUploader", "Evidence upload failed", exception)
            null
        }
    }

    private suspend fun createPlaceholderEvidenceFile(): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        file
    }

    private suspend fun uploadToFirebaseStorage(file: File): String =
        suspendCancellableCoroutine { continuation ->
            val reference = FirebaseStorage.getInstance().reference
                .child("evidence/${UUID.randomUUID()}.jpg")

            val metadata = storageMetadata {
                contentType = "image/jpeg"
            }

            reference.putFile(Uri.fromFile(file), metadata)
                .continueWithTask { uploadTask ->
                    if (!uploadTask.isSuccessful) {
                        uploadTask.exception?.let { throw it }
                    }
                    reference.downloadUrl
                }
                .addOnSuccessListener { uri -> continuation.resume(uri.toString()) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }
}
