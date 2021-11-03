/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.core.store.security

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
