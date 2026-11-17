package com.uvg.cc3087.myapp

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class NetworkConnectionTest {
    @Test fun deviceDisconnectAndReconnectUpdateAccountWithoutClaimingBackendSuccess() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val wifi = shell("settings get global wifi_on") == "1"
        val data = shell("settings get global mobile_data") == "1"
        val network = NetworkConnection(instrumentation.targetContext)
        val account = DefaultAccountSyncRepository(network.connected, { flowOf(AccountPendingChanges(drafts = 1)) },
            { AccountPendingChanges(drafts = 1) }, { "a" }, {}, synchronize = { SyncResult.SUCCESS })
        try {
            shell("svc wifi enable"); shell("svc data enable")
            withTimeout(30_000) { network.connected.first { it } }
            shell("svc wifi disable"); shell("svc data disable")
            withTimeout(30_000) { network.connected.first { !it } }
            val offline = withTimeout(10_000) { account.observe("a").first { it.phase == AccountSyncPhase.WAITING_CONNECTION } }
            assertEquals(1, offline.pending.drafts)
            assertNull(offline.lastSuccessfulCheck)
            assertEquals(SyncResult.RETRY, account.run("a"))
            shell("svc wifi enable"); shell("svc data enable")
            withTimeout(30_000) { network.connected.first { it } }
            val restored = account.observe("a").first()
            assertEquals(AccountSyncPhase.QUEUED, restored.phase)
            assertNull(restored.lastSuccessfulCheck)
        } finally {
            shell("svc wifi ${if (wifi) "enable" else "disable"}")
            shell("svc data ${if (data) "enable" else "disable"}")
        }
    }
}
