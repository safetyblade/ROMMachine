package com.swordfish.lemuroid.app.gemrating

import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.PublicKey
import java.security.Signature

/** Binary GEMC1 envelope: magic, signed sequence, exact JSON bytes and RSA signature. */
internal object GemSignedCatalogue {
    private val magic = "GEMC1".encodeToByteArray()
    const val MAX_BYTES = GemCataloguePackage.MAX_BYTES + 8192

    data class Verified(
        val sequence: Long,
        val digest: String,
        val bytes: ByteArray,
        val catalogue: GemCataloguePackage.Loaded,
    )

    fun read(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val byte = input.read()
                if (byte < 0) break
                output.write(byte)
            } else {
                output.write(buffer, 0, count)
            }
            require(output.size() <= MAX_BYTES) { "Catalogue update exceeds size limit" }
        }
        return output.toByteArray()
    }

    fun verify(
        bytes: ByteArray,
        key: PublicKey,
    ): Verified {
        require(bytes.size <= MAX_BYTES)
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val header = ByteArray(magic.size).also(input::readFully)
        require(header.contentEquals(magic))
        val sequence = input.readLong()
        require(sequence > 0)
        val length = input.readInt()
        require(length in 1..GemCataloguePackage.MAX_BYTES && length <= input.available())
        val payload = ByteArray(length).also(input::readFully)
        val signatureLength = input.readInt()
        require(signatureLength in 256..1024 && signatureLength == input.available())
        val signature = ByteArray(signatureLength).also(input::readFully)
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(key)
        verifier.update(magic)
        verifier.update(ByteBuffer.allocate(12).putLong(sequence).putInt(length).array())
        verifier.update(payload)
        require(verifier.verify(signature)) { "Catalogue signature not trusted" }
        val manifest =
            Json.decodeFromString(
                GemCataloguePackage.Manifest.serializer(),
                payload.decodeToString(throwOnInvalidSequence = true),
            )
        require(manifest.catalogueVersion.matches(Regex("[A-Za-z0-9._-]{1,128}")))
        require(manifest.releaseCount > 0)
        val approval =
            GemCataloguePackage.Approval(
                manifest.catalogueVersion,
                GemProfileBackup.digest(payload),
                manifest.sources,
            )
        val catalogue = GemCataloguePackage.load(payload.inputStream(), approval)
        return Verified(sequence, GemProfileBackup.digest(bytes), bytes, catalogue)
    }
}
