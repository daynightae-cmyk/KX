package com.kx.vpn.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class BoundedFileReaderTest {

    private fun read(text: String, max: Int) =
        BoundedFileReader.read(ByteArrayInputStream(text.toByteArray()), max)

    @Test
    fun returnsTheExactBytesOfAFileWithinTheLimit() {
        val outcome = read("[Interface]\nPrivateKey = abc\n", 4096)
        assertTrue(outcome is BoundedFileReader.Outcome.Read)
        assertEquals(
            "[Interface]\nPrivateKey = abc\n",
            String((outcome as BoundedFileReader.Outcome.Read).bytes),
        )
    }

    @Test
    fun acceptsAFileOfExactlyTheLimit() {
        val payload = "a".repeat(64)
        val outcome = read(payload, 64)
        assertTrue("exactly-at-limit must not be rejected", outcome is BoundedFileReader.Outcome.Read)
        assertEquals(64, (outcome as BoundedFileReader.Outcome.Read).bytes.size)
    }

    @Test
    fun rejectsOneByteOverTheLimitWithoutBufferingTheWholeStream() {
        val payload = "a".repeat(10_000)
        val outcome = read(payload, 64)
        assertEquals(BoundedFileReader.Outcome.OverLimit(64), outcome)
    }

    @Test
    fun anEndlessStreamIsStoppedAtTheLimit() {
        // A FIFO or device node masquerading as a file: the reader must give up at the
        // ceiling rather than accumulate forever.
        var produced = 0
        val endless = object : InputStream() {
            override fun read(): Int {
                produced++
                return if (produced > 200_000) -1 else 'a'.code
            }
        }
        val outcome = BoundedFileReader.read(endless, 4096)
        assertTrue("expected OverLimit, got $outcome", outcome is BoundedFileReader.Outcome.OverLimit)
        assertTrue(
            "reader must stop near the limit, but it pulled $produced bytes",
            produced < 200_000,
        )
    }

    @Test
    fun anEmptyFileIsReportedAsEmptyNotAsInvalidContent() {
        assertEquals(
            BoundedFileReader.Outcome.Failed(BoundedFileReader.FailureReason.Empty),
            read("", 4096),
        )
    }

    @Test
    fun ioFailuresBecomeUnreadableRatherThanPropagating() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("device gone")
        }
        assertEquals(
            BoundedFileReader.Outcome.Failed(BoundedFileReader.FailureReason.Unreadable),
            BoundedFileReader.read(broken, 4096),
        )
    }

    @Test
    fun revokedUriPermissionBecomesUnreadable() {
        val denied = object : InputStream() {
            override fun read(): Int = throw SecurityException("no permission")
        }
        assertEquals(
            BoundedFileReader.Outcome.Failed(BoundedFileReader.FailureReason.Unreadable),
            BoundedFileReader.read(denied, 4096),
        )
    }

    @Test
    fun aNonPositiveLimitIsAProgrammingErrorRatherThanAnAcceptance() {
        try {
            BoundedFileReader.read(ByteArrayInputStream("x".toByteArray()), 0)
            fail("expected require() to reject a zero limit")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun theStreamIsClosedEvenWhenTheLimitIsExceeded() {
        var closed = false
        val stream = object : ByteArrayInputStream("a".repeat(500).toByteArray()) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        BoundedFileReader.read(stream, 16)
        assertTrue("early return must still close the stream", closed)
    }
}
