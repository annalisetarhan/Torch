package com.annalisetarhan.torch.encryption

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

class Keys(context: Context) {
    private lateinit var keyPair: KeyPair
    private lateinit var prefs: EncryptedSharedPreferences

    init {
        /* Use the stored keypair if there is one */
        getEncryptedSharedPrefs(context)
        if (prefs.getString("public_key", null) != null
                && prefs.getString("private_key", null) != null) {
            getExistingKeyPair()
        } else {
            generateKeyPair()
        }
    }

    fun privateKey(): PrivateKey = keyPair.private
    fun publicKey(): PublicKey = keyPair.public

    fun truncatedPublicKey(): Long {
        val publicKey = keyPair.public as RSAPublicKey
        val truncated = publicKey.modulus % Long.MAX_VALUE.toBigInteger()
        return truncated.toLong()
    }

    private fun getExistingKeyPair() {
        val decoder = Base64.getDecoder()
        val keyFactory = KeyFactory.getInstance(Constants.KEY_PAIR_ALGORITHM)

        val publicString = prefs.getString("public_key", null)!!
        val publicBytes = decoder.decode(publicString)
        val publicSpec = X509EncodedKeySpec(publicBytes)
        val publicKey = keyFactory.generatePublic(publicSpec)

        val privateString = prefs.getString("private_key", null)!!
        val privateBytes = decoder.decode(privateString)
        val privateSpec = PKCS8EncodedKeySpec(privateBytes)
        val privateKey = keyFactory.generatePrivate(privateSpec)

        keyPair = KeyPair(publicKey, privateKey)
    }

    fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(Constants.KEY_PAIR_ALGORITHM)
        keyPairGenerator.initialize(Constants.RSA_KEY_BITS)
        keyPair = keyPairGenerator.generateKeyPair()

        val keyFactory = KeyFactory.getInstance(Constants.KEY_PAIR_ALGORITHM)
        val publicSpec = keyFactory.getKeySpec(keyPair.public, X509EncodedKeySpec::class.java)
        val privateSpec = keyFactory.getKeySpec(keyPair.private, PKCS8EncodedKeySpec::class.java)

        val encoder = Base64.getEncoder()
        val publicEncoded = encoder.encodeToString(publicSpec.encoded)
        val privateEncoded = encoder.encodeToString(privateSpec.encoded)

        val editor = prefs.edit()
        editor.putString("public_key", publicEncoded)
        editor.putString("private_key", privateEncoded)
        editor.apply()
    }

    private fun getEncryptedSharedPrefs(context: Context) {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        prefs = EncryptedSharedPreferences.create(
                "enc_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
}