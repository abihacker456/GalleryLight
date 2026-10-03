package com.abinet.gallerylight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import com.abinet.gallerylight.databinding.ActivityVideoPlayerBinding

@OptIn(UnstableApi::class)
class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "extra_uri"
    }

    private lateinit var b: ActivityVideoPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashlightOn = false
    private var controlsVisible = false
    private var currentSpeed = 1.0f
    private var currentEffectIndex = 0
    private var currentBrightness = 0.5f

    private val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val effectNames = listOf("Normal", "Grayscale", "Sepia", "Warm", "Cool")

    private val hideRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        b = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(b.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        findCameraWithFlash()

        currentSpeed = Prefs.speed(this)
        currentBrightness = Prefs.brightness(this) / 100f
        applyBrightness()

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = if (Prefs.loop(this@VideoPlayerActivity)) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            setPlaybackSpeed(currentSpeed)
        }
        b.playerView.player = player

        setupButtons()
        setupTouch()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })

        val uriString = intent.getStringExtra(EXTRA_URI)
        if (uriString == null) {
            ToastCompat(this, R.string.player_pick_video_first)
            finish(); return
        }
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
        player.prepare()
        if (Prefs.autoPlay(this)) player.play()
        b.btnSpeed.text = "⚡ ${currentSpeed}x"
        b.btnLoop.text = if (player.repeatMode == Player.REPEAT_MODE_ALL)
            getString(R.string.player_loop_on) else getString(R.string.player_loop_off)
    }

    private fun setupButtons() {
        b.btnClose.setOnClickListener { finish() }
        b.btnFlashlight.setOnClickListener { toggleFlashlight() }

        b.btnEffect.setOnClickListener {
            currentEffectIndex = (currentEffectIndex + 1) % effectNames.size
            applyEffect()
        }

        b.btnSpeed.setOnClickListener {
            val idx = speeds.indexOf(currentSpeed).let { if (it < 0) 2 else it }
            val next = (idx + 1) % speeds.size
            currentSpeed = speeds[next]
            player.setPlaybackSpeed(currentSpeed)
            b.btnSpeed.text = "⚡ ${currentSpeed}x"
        }

        b.btnLoop.setOnClickListener {
            if (player.repeatMode == Player.REPEAT_MODE_ALL) {
                player.repeatMode = Player.REPEAT_MODE_OFF
                b.btnLoop.text = getString(R.string.player_loop_off)
            } else {
                player.repeatMode = Player.REPEAT_MODE_ALL
                b.btnLoop.text = getString(R.string.player_loop_on)
            }
        }

        b.btnBrightnessDown.setOnClickListener {
            currentBrightness = (currentBrightness - 0.1f).coerceAtLeast(0.05f)
            applyBrightness()
        }
        b.btnBrightnessUp.setOnClickListener {
            currentBrightness = (currentBrightness + 0.1f).coerceAtMost(1f)
            applyBrightness()
        }

        b.btnPlay.setOnClickListener {
            if (player.isPlaying) { player.pause(); b.btnPlay.text = "▶" }
            else { player.play(); b.btnPlay.text = "⏸" }
        }

        b.btnRewind.setOnClickListener {
            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
        }
        b.btnForward.setOnClickListener {
            player.seekTo(player.currentPosition + 10_000)
        }
    }

    private fun setupTouch() {
        b.touchOverlay.setOnClickListener { toggleControls() }
        val buttons = listOf(
            b.btnEffect, b.btnSpeed, b.btnLoop, b.btnPlay,
            b.btnRewind, b.btnForward, b.btnBrightnessDown, b.btnBrightnessUp
        )
        buttons.forEach { btn ->
            btn.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) resetHideTimer()
                false
            }
        }
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        b.controlsContainer.visibility = View.VISIBLE
        b.topBar.visibility = View.VISIBLE
        resetHideTimer()
    }

    private fun hideControls() {
        controlsVisible = false
        b.controlsContainer.visibility = View.GONE
        b.topBar.visibility = View.GONE
    }

    private fun resetHideTimer() {
        b.touchOverlay.removeCallbacks(hideRunnable)
        b.touchOverlay.postDelayed(hideRunnable, 4000)
    }

    private fun applyEffect() {
        val effect: Effect? = when (effectNames[currentEffectIndex]) {
            "Grayscale" -> RgbMatrix { _, _ ->
                floatArrayOf(
                    0.299f, 0.587f, 0.114f, 0f,
                    0.299f, 0.587f, 0.114f, 0f,
                    0.299f, 0.587f, 0.114f, 0f,
                    0f, 0f, 0f, 1f
                )
            }
            "Sepia" -> RgbMatrix { _, _ ->
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f,
                    0.349f, 0.686f, 0.168f, 0f,
                    0.272f, 0.534f, 0.131f, 0f,
                    0f, 0f, 0f, 1f
                )
            }
            "Warm" -> RgbMatrix { _, _ ->
                floatArrayOf(
                    1.2f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f,
                    0f, 0f, 0.8f, 0f,
                    0f, 0f, 0f, 1f
                )
            }
            "Cool" -> RgbMatrix { _, _ ->
                floatArrayOf(
                    0.8f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f,
                    0f, 0f, 0f, 1f
                )
            }
            else -> null
        }
        player.setVideoEffects(if (effect == null) emptyList() else listOf(effect))
        b.btnEffect.text = "🎨 ${effectNames[currentEffectIndex]}"
    }

    private fun applyBrightness() {
        window.attributes = window.attributes.apply { screenBrightness = currentBrightness }
    }

    private fun toggleFlashlight() {
        val id = cameraId
        if (id == null) {
            ToastCompat(this, "No flashlight on this device"); return
        }
        try {
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(id, isFlashlightOn)
            b.btnFlashlight.text = if (isFlashlightOn) "💡" else "🔦"
        } catch (_: Exception) {}
    }

    private fun turnOffFlashlight() {
        val id = cameraId ?: return
        if (isFlashlightOn) {
            try { cameraManager.setTorchMode(id, false) } catch (_: Exception) {}
            isFlashlightOn = false
        }
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

    override fun onPause() {
        super.onPause()
        player.pause()
        turnOffFlashlight()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        turnOffFlashlight()
    }
}