package com.uvg.cc3087.myapp

import android.app.Application
import androidx.work.Configuration
import com.uvg.cc3087.myapp.di.FormSyncWorkerFactory

class FormLinkApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.TELEMETRY_ENABLED && !BuildConfig.FIREBASE_EMULATORS && com.google.firebase.FirebaseApp.initializeApp(this) != null) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(FormSyncWorkerFactory(this)).build()
}
