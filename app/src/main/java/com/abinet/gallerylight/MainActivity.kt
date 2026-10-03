package com.abinet.gallerylight

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.abinet.gallerylight.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private var isFlashlightOn = false
    private var hasFlash = false
    private var cameraId: String? = null

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startActivity(
                Intent(this, VideoPlayerActivity::class.java)
                    .putExtra(VideoPlayerActivity.EXTRA_URI, uri.toString())
            )
        }
    }

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val arr = ArrayList<String>(uris.size)
            uris.forEach { arr.add(it.toString()) }
            startActivity(
                Intent(this, ImageProjectionActivity::class.java)
                    .putStringArrayListExtra(ImageProjectionActivity.EXTRA_URIS, arr)
                    .putExtra(ImageProjectionActivity.EXTRA_FLASHLIGHT, isFlashlightOn)
            )
        } else {
            Toast.makeText(this, R.string.image_no_images, Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) toggleFlashlightInternal()
        else Toast.makeText(this, "Camera permission required for flashlight", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        detectFlash()

        b.toolbar.inflateMenu(R.menu.menu_home)
        b.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            } else false
        }

        b.cardVideo.setOnClickListener {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        b.cardImage.setOnClickListener {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        b.cardFlashlight.setOnClickListener { requestToggleFlashlight() }

        b.cardAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun detectFlash() {
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val flash = chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (flash) {
                    hasFlash = true
                    cameraId = id
                    break
                }
            }
        } catch (_: Exception) { hasFlash = false }
        if (!hasFlash) {
            b.flashlightState.text = "Not available on this device"
        }
    }

    private fun requestToggleFlashlight() {
        if (!hasFlash) {
            Toast.makeText(this, "This device has no flashlight", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPerm.launch(Manifest.permission.CAMERA)
        } else toggleFlashlightInternal()
    }

    private fun toggleFlashlightInternal() {
        val id = cameraId ?: return
        try {
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(id, isFlashlightOn)
            b.flashlightState.text = getString(
                if (isFlashlightOn) R.string.card_flashlight_on else R.string.card_flashlight_off
            )
            b.flashlightEmoji.text = if (isFlashlightOn) "💡" else "🔦"
        } catch (e: Exception) {
            Toast.makeText(this, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFlashlightOn) {
            val id = cameraId ?: return
            try {
                cameraManager.setTorchMode(id, false)
            } catch (_: Exception) {}
            isFlashlightOn = false
            b.flashlightState.text = getString(R.string.card_flashlight_off)
            b.flashlightEmoji.text = "🔦"
        }
    }
}