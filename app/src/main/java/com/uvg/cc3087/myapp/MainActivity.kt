package com.uvg.cc3087.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uvg.cc3087.myapp.ui.navigation.FormLinkApp
import com.uvg.cc3087.myapp.ui.theme.MyappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyappTheme {
                // main activity solo monta la app y deja la navegación en su propio archivo
                FormLinkApp()
            }
        }
    }
}
