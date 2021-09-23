package com.zuko.billingz.core.store

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object Securityz {

    fun encrypt(data: String): ByteArray {
        val key: SecretKey = keygen()
        val cipher = cipher(key)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val plaintext: ByteArray = data.toByteArray()
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        val iv = cipher.iv
        return ciphertext
    }

    fun decrypt(encryptedData: ByteArray): String {
        val key: SecretKey = keygen()
        val cipher = cipher(key)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encryptedData).decodeToString()
    }

    private fun keygen(): SecretKey {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        return keygen.generateKey()
    }

    private fun cipher(key: SecretKey): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        return cipher
    }
}
