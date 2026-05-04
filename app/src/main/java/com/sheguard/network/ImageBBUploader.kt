package com.sheguard.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ImageBBUploader(private val context: Context) {

    suspend fun uploadImage(imageUri: Uri?): String? = withContext(Dispatchers.IO) {
        if (imageUri == null) {
            return@withContext null
        }

        runCatching {
            val imageBase64 = optimizeAndEncode(imageUri)
            val requestBody = FormBody.Builder()
                .add("key", API_KEY)
                .add("image", imageBase64)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("ImgBB upload failed with code ${response.code}")
                }

                val responseBody = response.body?.string().orEmpty()
                val jsonObject = JSONObject(responseBody)
                jsonObject.getJSONObject("data").getString("url")
            }
        }.onFailure { exception ->
            Log.e(TAG, "ImgBB upload failed", exception)
        }.getOrNull()
    }

    private fun optimizeAndEncode(imageUri: Uri): String {
        val decodedBitmap = decodeBitmap(imageUri)
            ?: throw IllegalStateException("Unable to decode image for upload")
        val resizedBitmap = resizeBitmap(decodedBitmap)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val imageBytes = outputStream.toByteArray()

        if (resizedBitmap !== decodedBitmap) {
            resizedBitmap.recycle()
        }
        decodedBitmap.recycle()

        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }

    private fun decodeBitmap(imageUri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }

        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, MAX_WIDTH_PX)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH_PX) {
            return bitmap
        }

        val scaledHeight = (bitmap.height * (MAX_WIDTH_PX.toFloat() / bitmap.width)).toInt()
        return Bitmap.createScaledBitmap(bitmap, MAX_WIDTH_PX, scaledHeight, true)
    }

    private fun calculateInSampleSize(width: Int, maxWidth: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        while (currentWidth > maxWidth * 2) {
            sampleSize *= 2
            currentWidth /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    companion object {
        private const val TAG = "ImageBBUploader"
        private const val API_KEY = "45a00ca8fe266a43d267458cee99b16e"
        private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
        private const val MAX_WIDTH_PX = 1024
        private const val JPEG_QUALITY = 70
        private val okHttpClient = OkHttpClient()
    }
}
