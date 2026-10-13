package com.example.gallery

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import com.example.gallery.ui.GalleryApp
import com.example.gallery.ui.GalleryTheme
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GalleryApp(viewModel, onToggleLanguage = { toggleLanguage() })
                }
            }
        }
    }

    /** สลับภาษาในแอพระหว่างไทยกับอังกฤษ */
    private fun toggleLanguage() {
        val current = AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?: Locale.getDefault().language
        val next = if (current == "th") "en" else "th"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
    }
}
