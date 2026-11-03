package com.kx.vpn.domain.config

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads a user-selected configuration with a hard ceiling.
 *
 * The ceiling is enforced while streaming, not after: a hostile picker target (a FIFO,
 * a device node, a huge file) must not be able to make the app allocate unbounded
 * memory before validation runs. Reading one byte past the limit is deliberate — it
 * distinguishes "exactly at the cap" from "over the cap" without buffering the whole
 * thing.
 */
object BoundedFileReader {

    sealed interface Outcome {
        data class Read(val bytes: ByteArray) : Outcome
        data class OverLimit(val limitBytes: Int) : Outcome
        data class Failed(val reason: FailureReason) : Outcome
    }

    enum class FailureReason { Unreadable, Empty }

    fun read(stream: InputStream, maxBytes: Int): Outcome {
        require(maxBytes > 0) { "limit must be positive" }
        val buffer = ByteArrayOutputStream(maxBytes.coerceAtMost(16 * 1024))
        val chunk = ByteArray(8 * 1024)
        var total = 0

        try {
            while (true) {
                val read = stream.read(chunk)
                if (read == -1) break
                total += read
                if (total > maxBytes) return Outcome.OverLimit(maxBytes)
                buffer.write(chunk, 0, read)
            }
        } catch (expected: IOException) {
            return Outcome.Failed(FailureReason.Unreadable)
        } catch (expected: SecurityException) {
            // A revoked or never-granted URI permission surfaces here, not as an
            // IOException. Reporting it as "could not read" is accurate; reporting it
            // as an invalid configuration would not be.
            return Outcome.Failed(FailureReason.Unreadable)
        } finally {
            runCatching { stream.close() }
        }

        if (total == 0) return Outcome.Failed(FailureReason.Empty)
        return Outcome.Read(buffer.toByteArray())
    }
}
