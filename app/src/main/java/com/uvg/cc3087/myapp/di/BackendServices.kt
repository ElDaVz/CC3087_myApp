package com.uvg.cc3087.myapp.di

import android.content.Context
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.BuildConfig
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.userId
import com.uvg.cc3087.myapp.data.repository.*

/** Application-scoped construction only; repositories and workers receive interfaces. */
class BackendServices(private val context: Context, private val database: FormLinkDatabase, covers: CoverStorage) {
    private val firebase = if (BuildConfig.FIREBASE_EMULATORS) {
        FirebaseApp.getApps(context).find { it.name == "formlink-emulator" }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setProjectId("demo-formlink").setApplicationId("1:1234567890:android:formlink")
                .setApiKey("demo-api-key").build(), "formlink-emulator")
    } else FirebaseApp.getApps(context).find { it.name == FirebaseApp.DEFAULT_APP_NAME }
        ?: FirebaseApp.initializeApp(context)
    private val http = com.uvg.cc3087.myapp.data.remote.http.FirebaseHttp(BuildConfig.FIREBASE_EMULATORS)
    val deviceCipher = com.uvg.cc3087.myapp.data.security.DeviceCipher(context)
    private val guestImportStore = RoomGuestImportStore(database)
    val auth = RestAuthRepository(firebase?.options?.apiKey, deviceCipher, http, BuildConfig.FIREBASE_EMULATORS,
        guestImports = guestImportStore)
    val local: LocalFormStore = RoomFormStore(database, covers)
    val client = firebase?.let { com.uvg.cc3087.myapp.data.remote.http.FirestoreRest(it.options.projectId!!, auth, http, BuildConfig.FIREBASE_EMULATORS) }
    private val remote = client?.let { com.uvg.cc3087.myapp.data.remote.RestFormSource(it) }
    val analytics: com.uvg.cc3087.myapp.domain.telemetry.ProductAnalytics =
        if (firebase != null && BuildConfig.TELEMETRY_ENABLED && !BuildConfig.FIREBASE_EMULATORS)
            FirebaseProductAnalytics(context, database.analytics(), if (BuildConfig.DEBUG) "development" else "users")
        else com.uvg.cc3087.myapp.domain.telemetry.NoProductAnalytics
    init {
        if (analytics is FirebaseProductAnalytics) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            try { analytics.flush() } catch (_: Exception) { /* Retry ledger next startup. */ }
        }
    }
    val sync = remote?.let { FormSyncEngine(local, it) { auth.state.value.userId } }
    val publisher by lazy { client?.let { com.uvg.cc3087.myapp.data.remote.RestFormPublisher(it, checkNotNull(remote), database,
        { auth.state.value.userId }, { block -> checkNotNull(sync).exclusive(block) }, analytics, covers,
        if (BuildConfig.FIREBASE_EMULATORS) null else com.uvg.cc3087.myapp.data.remote.CloudinaryCoverUploader(BuildConfig.CLOUDINARY_CLOUD_NAME, BuildConfig.CLOUDINARY_UPLOAD_PRESET)) } }
    val guestImports: GuestImportRepository = GuestDraftImporter(guestImportStore, { auth.state.value.userId }, ::schedule)
    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            auth.state.map { it.userId }.distinctUntilChanged().collectLatest { owner ->
                if (owner != null) guestImports.resume(owner)
            }
        }
    }
    fun repository(owner: String?) = OfflineFirstFormRepository(local, auth, owner, publisher, ::schedule)
    fun requests(owner: String) = client?.let { CloudRequestRepository(database, it, owner, { auth.state.value.userId }, ::schedule) }

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
        if (!services.guestImports.resume(owner)) return Result.retry()
        if (services.auth.state.value.userId != owner) return Result.success()
        return when (services.sync?.sync(owner)) {
            SyncResult.RETRY -> Result.retry()
            SyncResult.SESSION_CHANGED -> Result.success()
            else -> try {
                services.publisher?.refresh(owner)
                services.requests(owner)?.sync()
                Result.success()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { services.sync?.schedulingFailed(owner); Result.retry() }
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
