package com.abinet.gallerylight

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.abinet.gallerylight.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var b: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        b.toolbar.setNavigationOnClickListener { finish() }

        b.btnFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${getString(R.string.about_email)}"))
                .putExtra(Intent.EXTRA_SUBJECT, "GalleryLight feedback")
            safeStart(intent)
        }

        b.btnRate.setOnClickListener {
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_store_url))))
        }

        b.btnShare.setOnClickListener {
            val text = getString(R.string.share_body) + getString(R.string.play_store_url)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(i, getString(R.string.about_share)))
        }

        b.btnPrivacy.setOnClickListener {
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url))))
        }
    }

    private fun safeStart(intent: Intent) {
        try { startActivity(intent) } catch (_: Exception) {}
    }
}