package me.rerere.rikkahub.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import me.rerere.rikkahub.reliability.SecretRedactor
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CredentialVault {
    fun encrypt(id: String, secret: CharArray)
    fun decrypt(id: String): CharArray?
    fun delete(id: String)
    fun rotate(id: String, secret: CharArray)
    fun contains(id: String): Boolean
}

/** AES-256-GCM envelope; only ciphertext/IV are persisted, while the key stays in Keystore. */
class AndroidCredentialVault(context: Context) : CredentialVault {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun encrypt(id: String, secret: CharArray) {
        require(id.matches(Regex("[a-zA-Z0-9_.-]{1,80}")))
        val bytes = secret.concatToString().toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
                Base64.encodeToString(cipher.doFinal(bytes), Base64.NO_WRAP)
            check(preferences.edit().putString(id, payload).commit()) { "Credential persistence failed" }
            SecretRedactor.registerKnownSecret(secret)
        } finally {
            bytes.fill(0)
        }
    }

    override fun decrypt(id: String): CharArray? {
        val value = preferences.getString(id, null) ?: return null
        val pieces = value.split('.', limit = 2)
        if (pieces.size != 2) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(pieces[0], Base64.NO_WRAP)))
        val plain = cipher.doFinal(Base64.decode(pieces[1], Base64.NO_WRAP))
        return try {
            plain.toString(Charsets.UTF_8).toCharArray().also {
                SecretRedactor.registerKnownSecret(it)
            }
        }
        finally { plain.fill(0) }
    }

    override fun delete(id: String) { preferences.edit().remove(id).commit() }
    override fun rotate(id: String, secret: CharArray) = encrypt(id, secret)
    override fun contains(id: String): Boolean = preferences.contains(id)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
            generateKey()
        }
    }

    companion object {
        private const val PREFS = "credential_vault_ciphertexts"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "moataz.agent.credentials.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GITHUB_PAT = "github.pat"
    }
}
