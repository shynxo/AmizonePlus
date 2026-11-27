package com.student.amizoneplus.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local storage:
 *  - credentials: AES-GCM encrypted with a key that never leaves the Android Keystore
 *  - data cache / LMS cache: JSON files in app storage (shown instantly, works offline)
 *  - settings & small flags: SharedPreferences
 */
class Storage(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("amizone", Context.MODE_PRIVATE)
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /* ── Credentials ── */

    private val keyAlias = "amizone_credentials_key"
    private val PLAIN = "plain"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    /**
     * Encrypts with the phone's Keystore. Some older phones (often Android 8 / 9) have a
     * buggy Keystore that throws; then the key is recreated once, and if it still fails the
     * value is kept unencrypted in the app's private storage (marked "plain"), so signing
     * in still works on those phones.
     */
    private fun seal(plain: ByteArray): Pair<String, String> {
        repeat(2) { attempt ->
            runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
                val enc = cipher.doFinal(plain)
                return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) to Base64.encodeToString(enc, Base64.NO_WRAP)
            }.onFailure {
                if (attempt == 0) runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias) }
            }
        }
        return PLAIN to Base64.encodeToString(plain, Base64.NO_WRAP)
    }

    private fun open(iv: String, data: String): ByteArray? = runCatching {
        if (iv == PLAIN) return@runCatching Base64.decode(data, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        cipher.doFinal(Base64.decode(data, Base64.NO_WRAP))
    }.getOrNull()

    fun saveCredentials(c: Credentials) {
        val (iv, data) = seal("${c.username}\u0000${c.password}".toByteArray())
        prefs.edit().putString("cred_iv", iv).putString("cred_data", data).apply()
    }

    fun loadCredentials(): Credentials? {
        val iv = prefs.getString("cred_iv", null) ?: return null
        val data = prefs.getString("cred_data", null) ?: return null
        val (u, p) = open(iv, data)?.let { String(it).split('\u0000', limit = 2) }?.takeIf { it.size == 2 } ?: return null
        return Credentials(u, p)
    }

    /** Small secrets (e.g. the email password), encrypted with the same Keystore key. */
    fun saveSecret(name: String, value: String) {
        val (iv, data) = seal(value.toByteArray())
        prefs.edit().putString("sec_${name}_iv", iv).putString("sec_${name}_data", data).apply()
    }

    fun loadSecret(name: String): String? {
        val iv = prefs.getString("sec_${name}_iv", null) ?: return null
        val data = prefs.getString("sec_${name}_data", null) ?: return null
        return open(iv, data)?.let { String(it) }
    }

    /* ── JSON files ── */

    private fun file(name: String) = File(app.filesDir, name)

    fun <T> writeJson(name: String, serializer: KSerializer<T>, value: T) {
        runCatching { file(name).apply { parentFile?.mkdirs() }.writeText(json.encodeToString(serializer, value)) }
    }

    fun <T> readJson(name: String, serializer: KSerializer<T>): T? =
        runCatching { file(name).takeIf { it.exists() }?.readText()?.let { json.decodeFromString(serializer, it) } }.getOrNull()

    fun saveCache(d: AmizoneData) = writeJson("cache.json", AmizoneData.serializer(), d)
    fun loadCache(): AmizoneData? = readJson("cache.json", AmizoneData.serializer())

    /* ── Settings & flags ── */

    fun loadSettings(): Settings =
        prefs.getString("settings", null)?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()

    fun saveSettings(s: Settings) = prefs.edit().putString("settings", json.encodeToString(Settings.serializer(), s)).apply()

    var alertedCourses: Set<String>
        get() = prefs.getStringSet("alerted", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("alerted", v).apply()

    var seenBadges: Set<String>
        get() = prefs.getStringSet("badges_seen", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("badges_seen", v).apply()

    var reminderIds: Set<String>
        get() = prefs.getStringSet("reminder_ids", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("reminder_ids", v).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
        app.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias) }
    }
}
