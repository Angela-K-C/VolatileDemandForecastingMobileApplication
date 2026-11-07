package com.example.volatiledemandforecasting.core.security

/** The pair the backend issues on sign-in and on every refresh. Both are replaced together. */
data class Tokens(val accessToken: String, val refreshToken: String)

interface TokenStore {
    fun load(): Tokens?
    fun save(tokens: Tokens)
    fun clear()
}
