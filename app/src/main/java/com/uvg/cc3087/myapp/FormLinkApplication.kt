package com.uvg.cc3087.myapp

import android.app.Application
import androidx.work.Configuration
import com.uvg.cc3087.myapp.di.FormSyncWorkerFactory

class FormLinkApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(FormSyncWorkerFactory(this)).build()
}
