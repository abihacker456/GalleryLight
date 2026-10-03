package com.abinet.gallerylight

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.abinet.gallerylight.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        b.toolbar.setNavigationOnClickListener { finish() }

        b.swAutoPlay.isChecked = Prefs.autoPlay(this)
        b.swLoop.isChecked = Prefs.loop(this)
        b.sliderSpeed.value = Prefs.speed(this).coerceIn(0.5f, 2.0f)
        b.seekBrightness.progress = Prefs.brightness(this)

        b.sliderSpeed.addOnChangeListener { _, value, _ ->
            b.txtSpeedLabel.text = "Default speed: ${value}x"
        }
        b.txtSpeedLabel.text = "Default speed: ${b.sliderSpeed.value}x"

        b.seekBrightness.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                b.txtBrightness.text = "Default brightness: $progress%"
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        b.txtBrightness.text = "Default brightness: ${b.seekBrightness.progress}%"

        b.btnSave.setOnClickListener {
            Prefs.save(
                this,
                autoPlay = b.swAutoPlay.isChecked,
                loop = b.swLoop.isChecked,
                speed = b.sliderSpeed.value,
                brightness = b.seekBrightness.progress
            )
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        }
    }
}