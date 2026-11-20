package com.uvg.cc3087.myapp.ui.navigation

import com.uvg.cc3087.myapp.data.model.RequestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AppRouteTest {
    @Test fun everyDestinationCanBeRestoredWithItsArguments() {
        val routes = listOf(
            AppRoute.Forms, AppRoute.ChooseTemplate, AppRoute.Editor("draft-a"),
            AppRoute.Preview("draft-a"), AppRoute.SaveShare("draft-a"), AppRoute.Dashboard,
            AppRoute.Responses(InboxFilter.COMPLETED),
            AppRoute.RequestDetail("form-b", "response-b"), AppRoute.Account,
            AppRoute.Settings, AppRoute.Statistics
        )
        assertEquals(routes, Json.decodeFromString<List<AppRoute>>(Json.encodeToString(routes)))
    }

    @Test fun destinationIdsCannotBeBlank() {
        val invalidRoutes = listOf<() -> AppRoute>(
            { AppRoute.Editor("") }, { AppRoute.Preview(" ") }, { AppRoute.SaveShare("") },
            { AppRoute.RequestDetail("", "response") }, { AppRoute.RequestDetail("form", " ") }
        )
        invalidRoutes.forEach { factory ->
            assertThrows(IllegalArgumentException::class.java) { factory() }
        }
    }

    @Test fun inboxFiltersMatchKnownAndUnknownStatuses() {
        RequestStatus.entries.forEach { status ->
            assertTrue(InboxFilter.ALL.matches(status))
            assertTrue(InboxFilter.fromKey(status.name).matches(status))
            assertFalse(InboxFilter.UNKNOWN.matches(status))
        }
        assertTrue(InboxFilter.ALL.matches(null))
        assertTrue(InboxFilter.UNKNOWN.matches(null))
        assertFalse(InboxFilter.PENDING.matches(null))
        assertFalse(InboxFilter.COMPLETED.matches(RequestStatus.PENDING))
    }

    @Test fun anUnknownFilterKeyFallsBackToAll() {
        assertEquals(InboxFilter.ALL, InboxFilter.fromKey("obsolete"))
    }
}
