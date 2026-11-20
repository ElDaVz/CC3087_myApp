package com.uvg.cc3087.myapp

import android.os.ParcelFileDescriptor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
        val connectivity = instrumentation.targetContext.getSystemService(ConnectivityManager::class.java)
        fun connected() = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val originallyConnected = connected()
        val originalCapabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val transports = listOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET, NetworkCapabilities.TRANSPORT_VPN)
        fun originalTransportRestored(): Boolean {
            val current = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
            return transports.all { current.hasTransport(it) == (originalCapabilities?.hasTransport(it) == true) }
        }
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
            withContext(NonCancellable) {
                try {
                    shell("svc wifi ${if (wifi) "enable" else "disable"}")
                } finally {
                    shell("svc data ${if (data) "enable" else "disable"}")
                }
                withTimeout(30_000) {
                    while (connected() != originallyConnected) delay(200)
                    if (originallyConnected) {
                        // Network callbacks can precede route recovery. Do not let the next
                        // test attempt signup until the host Auth emulator is reachable.
                        withContext(Dispatchers.IO) {
                            var stableNetwork = connectivity.activeNetwork
                            var readyChecks = 0
                            while (readyChecks < 5) {
                                val active = connectivity.activeNetwork
                                val ready = connected() && originalTransportRestored() && authEmulatorReachable()
                                readyChecks = if (ready && active == stableNetwork && active == connectivity.activeNetwork)
                                    readyChecks + 1 else 0
                                stableNetwork = connectivity.activeNetwork
                                delay(200)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authEmulatorReachable(): Boolean {
        val connection = URL("http://10.0.2.2:9099/").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 1_000
            connection.readTimeout = 1_000
            connection.setRequestProperty("Connection", "close")
            // A 404 from this root URL also confirms that HTTP transport recovered.
            connection.responseCode in 100..599
        } catch (_: IOException) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
