package com.abinet.gallerylight

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.abinet.gallerylight.databinding.ActivityImageProjectionBinding

class ImageProjectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URIS = "extra_uris"
        const val EXTRA_FLASHLIGHT = "extra_flashlight"
    }

    private lateinit var b: ActivityImageProjectionBinding
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashlightOn = false
    private val uris = mutableListOf<Uri>()
    private var controlsVisible = false
    private var effectIndex = 0

    private val effectNames = listOf("Normal", "Grayscale", "Sepia", "Invert")
    private val adapter = ImageAdapter()

    private val hideRunnable = Runnable { hideControls() }

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { result ->
        if (result.isNotEmpty()) {
            uris.clear()
            uris.addAll(result)
            adapter.notifyDataSetChanged()
            updateInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        b = ActivityImageProjectionBinding.inflate(layoutInflater)
        setContentView(b.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        findCameraWithFlash()
        isFlashlightOn = intent.getBooleanExtra(EXTRA_FLASHLIGHT, false)

        val incoming = intent.getStringArrayListExtra(EXTRA_URIS).orEmpty()
        uris.clear()
        incoming.forEach { uris.add(Uri.parse(it)) }

        b.pager.adapter = adapter
        adapter.onScaleChanged = { scale ->
            b.pager.isUserInputEnabled = scale <= 1.05f
        }
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { updateInfo() }
        })

        setupButtons()
        b.touchOverlay.setOnClickListener { toggleControls() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })

        if (isFlashlightOn) turnOnFlashlight()
        updateInfo()
        b.btnEffect.text = "🎨 ${effectNames[effectIndex]}"
    }

    private fun setupButtons() {
        b.btnClose.setOnClickListener { finish() }
        b.btnFlashlight.setOnClickListener {
            if (isFlashlightOn) turnOffFlashlight() else turnOnFlashlight()
        }
        b.btnEffect.setOnClickListener {
            effectIndex = (effectIndex + 1) % effectNames.size
            adapter.notifyDataSetChanged()
            b.btnEffect.text = "🎨 ${effectNames[effectIndex]}"
        }
        b.btnPick.setOnClickListener {
            pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.btnPrev.setOnClickListener {
            if (b.pager.currentItem > 0) b.pager.currentItem -= 1
        }
        b.btnNext.setOnClickListener {
            if (b.pager.currentItem < uris.size - 1) b.pager.currentItem += 1
        }
    }

    private fun updateInfo() {
        if (uris.isEmpty()) {
            b.infoText.text = getString(R.string.image_no_images)
        } else {
            b.infoText.text = "${b.pager.currentItem + 1} / ${uris.size}"
        }
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        b.controlsContainer.visibility = View.VISIBLE
        b.topBar.visibility = View.VISIBLE
        b.touchOverlay.removeCallbacks(hideRunnable)
        b.touchOverlay.postDelayed(hideRunnable, 4000)
    }

    private fun hideControls() {
        controlsVisible = false
        b.controlsContainer.visibility = View.GONE
        b.topBar.visibility = View.GONE
    }

    private fun turnOnFlashlight() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, true)
            isFlashlightOn = true
            b.btnFlashlight.text = "💡"
        } catch (_: Exception) {}
    }

    private fun turnOffFlashlight() {
        val id = cameraId ?: return
        try { cameraManager.setTorchMode(id, false) } catch (_: Exception) {}
        isFlashlightOn = false
        b.btnFlashlight.text = "🔦"
    }

    private fun findCameraWithFlash() {
        try {
            for (id in cameraManager.cameraIdList) {
                val flash = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (flash) { cameraId = id; return }
            }
        } catch (_: Exception) {}
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        turnOffFlashlight()
    }

    private fun buildFilter(): ColorMatrixColorFilter? {
        val cm = when (effectNames[effectIndex]) {
            "Grayscale" -> ColorMatrix().apply { setSaturation(0f) }
            "Sepia" -> ColorMatrix().apply {
                set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            "Invert" -> ColorMatrix().apply {
                set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> null
        }
        return cm?.let { ColorMatrixColorFilter(it) }
    }

    private inner class ImageAdapter : RecyclerView.Adapter<ImageAdapter.VH>() {

        var onScaleChanged: ((Float) -> Unit)? = null

        inner class VH(val zoom: ZoomableImageView) : RecyclerView.ViewHolder(zoom)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val container = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val iv = ZoomableImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                onScaleChanged = { scale -> this@ImageAdapter.onScaleChanged?.invoke(scale) }
            }
            container.addView(iv)
            return VH(iv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val uri = uris[position]
            holder.zoom.resetTransform()
            holder.zoom.colorFilter = buildFilter()
            holder.zoom.load(uri) {
                crossfade(true)
                allowHardware(false)
                size(2048, 2048)
            }
        }

        override fun getItemCount() = uris.size
    }
}