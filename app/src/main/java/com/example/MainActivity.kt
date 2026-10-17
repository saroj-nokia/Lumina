package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.WallpaperDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.data.repository.WallhavenWallpaperRepository
import com.example.ui.MainApp
import com.example.ui.screens.favorites.FavoritesViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.preview.PreviewViewModel
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.LuminaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LuminaApp
        val appPreferences = AppPreferences(applicationContext)
        val repository = app.repository

        setContent {
            val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val isDynamicColor by appPreferences.isDynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LuminaTheme(darkTheme = isDark, dynamicColor = isDynamicColor) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(repository)
                )
                val previewViewModel: PreviewViewModel = viewModel(
                    factory = PreviewViewModel.Factory(repository)
                )
                val favoritesViewModel: FavoritesViewModel = viewModel(
                    factory = FavoritesViewModel.Factory(repository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(appPreferences, repository)
                )

                MainApp(
                    homeViewModel = homeViewModel,
                    previewViewModel = previewViewModel,
                    favoritesViewModel = favoritesViewModel,
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
