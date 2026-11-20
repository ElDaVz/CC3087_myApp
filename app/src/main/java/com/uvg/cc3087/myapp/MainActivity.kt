package com.uvg.cc3087.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import com.uvg.cc3087.myapp.di.AppContainer
import com.uvg.cc3087.myapp.ui.theme.AppearanceRoot
import com.uvg.cc3087.myapp.ui.navigation.FormLinkApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContainer.initialize(applicationContext)
        setContent {
            AppearanceRoot(AppContainer.appearanceRepository, onDarkThemeChanged = { dark ->
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.argb(230, 255, 255, 255), Color.argb(128, 0, 0, 0)) { dark }
                )
            }) {
                // main activity solo monta la app y deja la navegación en su propio archivo
                FormLinkApp()
            }
        }
    }
}
