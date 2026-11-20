package com.uvg.cc3087.myapp.ui.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.*
import org.junit.Test

class AppNavigatorTest {
    private val stack = mutableListOf<NavKey>(AppRoute.Forms)
    private val navigator = AppNavigator(stack)

    @Test fun rootCannotBePopped() {
        navigator.back()
        assertEquals(listOf(AppRoute.Forms), stack)
    }

    @Test fun consecutiveNavigationToTheSameDestinationIsNotDuplicated() {
        repeat(2) { navigator.navigate(AppRoute.ChooseTemplate) }
        assertEquals(listOf(AppRoute.Forms, AppRoute.ChooseTemplate), stack)
    }

    @Test fun backFollowsTheActualHistoryInsteadOfAFixedReturnScreen() {
        navigator.navigate(AppRoute.Dashboard)
        navigator.navigate(AppRoute.Responses(InboxFilter.PENDING))
        navigator.navigate(AppRoute.RequestDetail("form-a", "response-a"))
        navigator.back()
        assertTrue(navigator.isCurrent(AppRoute.Responses(InboxFilter.PENDING)))
        navigator.back()
        assertTrue(navigator.isCurrent(AppRoute.Dashboard))
    }

    @Test fun callbackFromAnOutgoingScreenDoesNotPopTheNextScreen() {
        navigator.navigate(AppRoute.ChooseTemplate)
        navigator.navigate(AppRoute.Editor("draft"))
        navigator.backFrom(AppRoute.ChooseTemplate)
        assertTrue(navigator.isCurrent(AppRoute.Editor("draft")))
        navigator.backFrom(AppRoute.Editor("draft"))
        assertTrue(navigator.isCurrent(AppRoute.ChooseTemplate))
    }

    @Test fun savingFromPreviewReturnsToTheEditorWhenTheReceiptIsClosed() {
        navigator.navigate(AppRoute.Editor("draft"))
        navigator.navigate(AppRoute.Preview("draft"))
        navigator.showSaveShare("draft")
        assertEquals(listOf(AppRoute.Forms, AppRoute.Editor("draft"), AppRoute.SaveShare("draft")), stack)
        navigator.back()
        assertTrue(navigator.isCurrent(AppRoute.Editor("draft")))
    }

    @Test fun sharingFromFormsReturnsDirectlyToForms() {
        navigator.showSaveShare("draft")
        navigator.back()
        assertEquals(listOf(AppRoute.Forms), stack)
    }

    @Test fun returningToFormsClearsIntermediateDestinations() {
        navigator.navigate(AppRoute.ChooseTemplate)
        navigator.navigate(AppRoute.Editor("draft"))
        navigator.showSaveShare("draft")
        navigator.showForms()
        assertEquals(listOf(AppRoute.Forms), stack)
        assertFalse(navigator.isCurrent(AppRoute.Editor("draft")))
    }

    @Test fun responseTabStartsFromFormsWithoutAnAbandonedTemplateFlow() {
        navigator.navigate(AppRoute.ChooseTemplate)
        navigator.showDashboard()
        navigator.showDashboard()
        assertEquals(listOf(AppRoute.Forms, AppRoute.Dashboard), stack)
    }

    @Test fun aRestoredStackKeepsItsIdsAndHistory() {
        val restored = mutableListOf<NavKey>(
            AppRoute.Forms, AppRoute.Editor("stored"), AppRoute.Preview("stored")
        )
        AppNavigator(restored).back()
        assertEquals(listOf(AppRoute.Forms, AppRoute.Editor("stored")), restored)
    }

    @Test fun aStackWithoutTheFormsRootIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { AppNavigator(mutableListOf()) }
    }

    @Test fun settingsReturnsToEachOriginAndRepeatedTapsDoNotDuplicateIt() {
        listOf(AppRoute.Forms, AppRoute.ChooseTemplate, AppRoute.Dashboard, AppRoute.Responses()).forEach { origin ->
            navigator.showForms()
            navigator.navigate(origin)
            navigator.showSettings()
            navigator.showSettings()
            assertEquals(1, stack.count { it == AppRoute.Settings })
            navigator.back()
            assertTrue(navigator.isCurrent(origin))
        }
    }

    @Test fun statisticsAndAccountReturnToSettingsAndFormsClearsTheHistory() {
        navigator.navigate(AppRoute.Dashboard)
        navigator.showSettings()
        navigator.navigate(AppRoute.Statistics)
        navigator.back()
        assertTrue(navigator.isCurrent(AppRoute.Settings))
        navigator.navigate(AppRoute.Account)
        navigator.back()
        assertTrue(navigator.isCurrent(AppRoute.Settings))
        navigator.navigate(AppRoute.Statistics)
        navigator.showSettings()
        assertEquals(listOf(AppRoute.Forms, AppRoute.Dashboard, AppRoute.Settings), stack)
        navigator.navigate(AppRoute.Statistics)
        navigator.showForms()
        assertEquals(listOf(AppRoute.Forms), stack)
    }
}
