package com.uvg.cc3087.myapp

import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.data.model.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun testUserSessionData() {
        val session = UserSession(
            userId = "usr_12345",
            email = "test@example.com",
            displayName = "Test User",
            isGuest = false
        )

        assertEquals("usr_12345", session.userId)
        assertEquals("test@example.com", session.email)
        assertEquals("Test User", session.displayName)
        assertFalse(session.isGuest)
    }

    @Test
    fun testGuestUserSession() {
        val session = UserSession(
            userId = "guest_99",
            email = "invitado@formlink.app",
            displayName = "Invitado",
            isGuest = true
        )

        assertTrue(session.isGuest)
        assertEquals("guest_99", session.userId)
    }

    @Test
    fun testAuthStateSealedClass() {
        val session = UserSession(userId = "123", email = "a@b.com", displayName = "A B")
        val state: AuthState = AuthState.Authenticated(session)

        assertTrue(state is AuthState.Authenticated)
        val authenticatedState = state as AuthState.Authenticated
        assertEquals("123", authenticatedState.session.userId)
    }
}
