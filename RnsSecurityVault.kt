package com.rns.controller.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.rns.controller.domain.model.TrustedDeviceRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class VaultPayload(
    val controllerId: String = UUID.randomUUID().toString(),
    val pinEnabled: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = "",
    val locked: Boolean = false,
    val recoveryKeySalt: String = "",
    val recoveryKeyHash: String = "",
    val recoveryHint: String = "",
    val trustedDevices: List<TrustedDeviceRecord> = emptyList()
)

object RnsSecurityCrypto {
    private val rng = SecureRandom()

    fun generateSaltHex(bytes: Int = 16): String {
        val buf = ByteArray(bytes)
        rng.nextBytes(buf)
        return buf.joinToString("") { "%02X".format(it) }
    }

    fun generateAuthToken(): String = generateSaltHex(16)

    fun generateRecoveryKey(): String {
        val p1 = "%04X".format(rng.nextInt(0x10000))
        val p2 = "%04X".format(rng.nextInt(0x10000))
        return "RNS-REC-$p1-$p2"
    }

    fun sha256Hex(secret: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$secret".toByteArray(Charsets.UTF_8)
        return md.digest(input).joinToString("") { "%02x".format(it) }
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        val ba = a.toByteArray(Charsets.UTF_8)
        val bb = b.toByteArray(Charsets.UTF_8)
        if (ba.size != bb.size) return false
        var diff = 0
        for (i in ba.indices) {
            diff = diff or (ba[i].toInt() xor bb[i].toInt())
        }
        return diff == 0
    }

    fun verifySecret(candidate: String, saltHex: String, expectedHashHex: String): Boolean {
        if (candidate.isBlank() || saltHex.isBlank() || expectedHashHex.isBlank()) return false
        val actual = sha256Hex(candidate.trim(), saltHex)
        return constantTimeEquals(actual, expectedHashHex)
    }
}

class RnsSecurityVault(context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "rns_security_vault_aes_key"
        private const val PREFS_NAME = "rns_security_vault_enc"
        private const val KEY_CIPHER_BLOB = "vault_blob_v1"
        private const val GCM_TAG_BITS = 128
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    fun loadVault(): VaultPayload {
        val blob = prefs.getString(KEY_CIPHER_BLOB, null)
        if (blob.isNullOrBlank()) {
            return VaultPayload().also { saveVault(it) }
        }
        return runCatching {
            val parts = blob.split(":")
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            json.decodeFromString<VaultPayload>(plain)
        }.getOrElse {
            VaultPayload().also { saveVault(it) }
        }
    }

    fun saveVault(payload: VaultPayload) {
        val encoded = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val encB64 = Base64.encodeToString(cipher.doFinal(encoded), Base64.NO_WRAP)
            prefs.edit().putString(KEY_CIPHER_BLOB, "$ivB64:$encB64").apply()
        }
    }
}
