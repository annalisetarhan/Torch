package com.annalisetarhan.torch.encryption

class Constants {
    companion object {
        const val SECONDS_IN_A_DAY = 86400
        const val SECONDS_IN_AN_HOUR = 3600

        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val RSA_KEY_BITS = 1024
        const val TRUNC_KEY_BYTES = Long.SIZE_BYTES
        const val TIMESTAMP_BYTES = 8
        const val MAX_MESSAGE_SIZE = 400

        const val CIPHER_TRANSFORMATION_STANDARD = "AES/GCM/NoPadding"
        const val CIPHER_TRANSFORMATION_PRIVATE = "RSA/ECB/PKCS1Padding"
        const val KEY_PAIR_ALGORITHM = "RSA"
        const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"  // Does this actually need to be 256? It's only for making hashkeys from hashtagsand creating message digests of already encrypted messages. It would save 16 bytes in msgs...
        const val SECRET_KEY_ALGORITHM = "AES"
    }
}