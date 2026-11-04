package com.uvg.cc3087.myapp.di

import android.content.Context
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.flow.first
import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.BuildConfig
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.remote.FirestoreFormSource
import com.uvg.cc3087.myapp.data.model.userId
import com.uvg.cc3087.myapp.data.repository.*

/** Application-scoped construction only; repositories and workers receive interfaces. */
class BackendServices(private val context: Context, database: FormLinkDatabase, covers: CoverStorage) {
    private val firebase = if (BuildConfig.FIREBASE_EMULATORS) {
        FirebaseApp.getApps(context).find { it.name == "formlink-emulator" }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setProjectId("demo-formlink").setApplicationId("1:1234567890:android:formlink")
                .setApiKey("demo-api-key").build(), "formlink-emulator")
    } else FirebaseApp.getApps(context).find { it.name == FirebaseApp.DEFAULT_APP_NAME }
        ?: FirebaseApp.initializeApp(context)
    private val firebaseAuth = firebase?.let { FirebaseAuth.getInstance(it) }?.apply {
        if (BuildConfig.FIREBASE_EMULATORS) useEmulator("10.0.2.2", 9099)
    }
    val auth: AuthRepository = FirebaseAuthRepository(firebaseAuth)
    val local: LocalFormStore = RoomFormStore(database, covers)
    private val remote = firebase?.let { app ->
        val firestore = FirebaseFirestore.getInstance(app)
        // Room is the durable outbox. Do not create a second disk outbox in the SDK.
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        if (BuildConfig.FIREBASE_EMULATORS) firestore.useEmulator("10.0.2.2", 8080)
        FirestoreFormSource(firestore)
    }
    val sync = remote?.let { FormSyncEngine(local, it) { auth.state.value.userId } }
    fun repository(owner: String?) = OfflineFirstFormRepository(local, auth, owner, ::schedule)
    fun schedule(owner: String) {
        if (sync == null || auth.state.value.userId != owner) return
        val request = OneTimeWorkRequestBuilder<FormSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("owner" to owner)).addTag("form-sync-$owner").build()
        try {
            WorkManager.getInstance(context).enqueueUniqueWork("form-sync-$owner", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        } catch (_: RuntimeException) {
            // A scheduler failure cannot undo a committed local edit.
            sync?.schedulingFailed(owner)
        }
    }
    fun cancel(owner: String) { WorkManager.getInstance(context).cancelAllWorkByTag("form-sync-$owner") }
}

class FormSyncWorker(context: Context, params: WorkerParameters, private val services: BackendServices) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString("owner") ?: return Result.failure()
        services.auth.state.first { it !is AuthState.Initializing }
        if (services.auth.state.value.userId != owner) return Result.success()
        return when (services.sync?.sync(owner)) {
            SyncResult.RETRY -> Result.retry()
            else -> Result.success()
        }
    }
}

class FormSyncWorkerFactory(private val context: Context) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        if (workerClassName != FormSyncWorker::class.java.name) return null
        AppContainer.initialize(context)
        return FormSyncWorker(appContext, workerParameters, AppContainer.backend)
    }
}
