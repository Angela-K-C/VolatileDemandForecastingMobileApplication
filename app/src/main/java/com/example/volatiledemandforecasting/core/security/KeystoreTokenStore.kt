package com.example.volatiledemandforecasting.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the tokens encrypted with an AES-GCM key that lives in the Android Keystore and never
 * leaves it (spec §14). Only the ciphertext reaches disk. If the key is lost or the blob is
 * unreadable, the user is simply signed out.
 */
class KeystoreTokenStore(context: Context) : TokenStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var cached: Tokens? = null
    private var loaded = false

    @Synchronized
    override fun load(): Tokens? {
        if (!loaded) {
            cached = readFromDisk()
            loaded = true
        }
        return cached
    }

    @Synchronized
    override fun save(tokens: Tokens) {
        prefs.edit().putString(KEY_BLOB, encrypt("${tokens.accessToken}\n${tokens.refreshToken}")).apply()
        cached = tokens
        loaded = true
    }

    @Synchronized
    override fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
        cached = null
        loaded = true
    }

    private fun readFromDisk(): Tokens? {
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val (access, refresh) = decrypt(blob).split('\n', limit = 2)
            Tokens(access, refresh)
        } catch (e: Exception) {
            prefs.edit().remove(KEY_BLOB).apply()
            null
        }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + sealed, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val bytes = Base64.decode(blob, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "secure_session"
        const val KEY_BLOB = "tokens"
        const val KEY_ALIAS = "vdf_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
