package com.sheguard.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class CaptureActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            if (intent.getBooleanExtra(EXTRA_TRIGGER_SOS_ON_RESULT, false)) {
                com.sheguard.sos.SOSManager(this).triggerSOS(evidenceUri = photoUri)
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_IMAGE_URI, photoUri.toString()))
            } else {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_IMAGE_URI, photoUri.toString()))
            }
        } else {
            if (intent.getBooleanExtra(EXTRA_TRIGGER_SOS_ON_RESULT, false)) {
                com.sheguard.sos.SOSManager(this).triggerSOS(shareOnlyLocation = true)
            }
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_TRIGGER_SOS_ON_RESULT = "triggerSosOnResult"
    }
}
