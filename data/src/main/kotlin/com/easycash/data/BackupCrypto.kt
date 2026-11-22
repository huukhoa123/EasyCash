package com.easycash.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupCrypto {
    private val magic = byteArrayOf(69, 67, 66, 1)
    // shortcut: backup tối đa 32 MiB trong RAM, chuyển sang streaming khi dữ liệu lớn hơn.
    const val MAX_BYTES = 32 * 1024 * 1024

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(plain.size <= MAX_BYTES && password.size in 8..1024)
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        return magic + salt + nonce + crypt(Cipher.ENCRYPT_MODE, plain, password, salt, nonce)
    }

    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(bytes.size in 48..MAX_BYTES + 48 && password.size in 8..1024)
        require(bytes.copyOfRange(0, 4).contentEquals(magic))
        return crypt(Cipher.DECRYPT_MODE, bytes.copyOfRange(32, bytes.size), password,
            bytes.copyOfRange(4, 20), bytes.copyOfRange(20, 32))
    }

    private fun crypt(mode: Int, bytes: ByteArray, password: CharArray, salt: ByteArray, nonce: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, 600_000, 256)
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(magic)
            return cipher.doFinal(bytes)
        } finally {
            key.fill(0)
        }
    }
}
