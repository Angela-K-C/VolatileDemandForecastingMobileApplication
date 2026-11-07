package com.example.volatiledemandforecasting.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SignOutReason {
    /** The refresh token was rejected: it expired, was revoked, or the account signed in elsewhere. */
    SessionExpired,
}

sealed interface SessionState {
    data class SignedOut(val reason: SignOutReason? = null) : SessionState
    data object SignedIn : SessionState
}

/**
 * The single owner of "who is signed in". It is read synchronously at start-up, so the first
 * screen is chosen before anything is drawn. Safe to call from any thread, including OkHttp's.
 */
class SessionManager(private val store: TokenStore) {

    private val _state = MutableStateFlow<SessionState>(
        if (store.load() != null) SessionState.SignedIn else SessionState.SignedOut()
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val tokens: Tokens? get() = store.load()

    fun signIn(tokens: Tokens) {
        store.save(tokens)
        _state.value = SessionState.SignedIn
    }

    /** A refresh rotates both tokens. The user stays signed in. */
    fun replaceTokens(tokens: Tokens) = store.save(tokens)

    fun signOut(reason: SignOutReason? = null) {
        store.clear()
        _state.value = SessionState.SignedOut(reason)
    }

    /** The login screen calls this once it has shown the reason, so it isn't shown twice. */
    fun acknowledgeSignOutReason() {
        _state.update { if (it is SessionState.SignedOut) SessionState.SignedOut() else it }
    }
}
