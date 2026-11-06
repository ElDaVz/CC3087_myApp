package com.uvg.cc3087.myapp.debug

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import com.uvg.cc3087.myapp.BuildConfig

/** Explicit QA-only demo; absent from release builds and normal navigation. */
class CrashlyticsDemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Button(this).apply {
            text = "DEMO QA: provocar fallo de prueba"
            isEnabled = BuildConfig.TELEMETRY_ENABLED && !BuildConfig.FIREBASE_EMULATORS
            setOnClickListener { throw IllegalStateException("FormLink controlled QA crash") }
        })
    }
}
