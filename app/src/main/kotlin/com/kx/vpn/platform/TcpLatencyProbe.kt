package com.kx.vpn.platform

import com.kx.vpn.domain.servers.EndpointMeasurement
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.EndpointValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

/**
 * Measures real latency by opening a TCP connection to the endpoint.
 *
 * This is a genuine measurement: the socket is opened and the elapsed time is
 * recorded, so the number shown is the time the machine actually took to reach that
 * host and port. Nothing is estimated, interpolated or carried over from another
 * endpoint.
 *
 * Two bounds keep it from becoming a resource-heavy scan loop:
 *
 * - a hard ceiling on how many endpoints one batch may probe, and
 * - a concurrency limit, so a large catalogue cannot open dozens of sockets at once.
 *
 * The host is validated before it reaches a socket, so an endpoint with a hostile
 * hostname is refused rather than resolved.
 */
/**
 * Something that can tell whether the user's own endpoints are reachable, and how long
 * that took.
 *
 * An interface so the screen's behaviour under measurement — offline refusal, stale
 * handling, the measuring flag — is testable without opening a single socket. The only
 * production implementation is [TcpLatencyProbe], which really connects.
 */
interface LatencyProbe {
    suspend fun measure(profiles: List<EndpointProfile>): Map<String, EndpointMeasurement>
}

class TcpLatencyProbe(
    private val connectTimeoutMillis: Int = 3_000,
    private val maxConcurrent: Int = 4,
    private val maxEndpointsPerBatch: Int = 32,
    private val now: () -> Long = System::currentTimeMillis,
) : LatencyProbe {

    override suspend fun measure(profiles: List<EndpointProfile>): Map<String, EndpointMeasurement> =
        withContext(Dispatchers.IO) {
            val measurable = profiles
                .filter { EndpointValidator.isValidHost(it.host) && it.port in 1..65535 }
                .take(maxEndpointsPerBatch)

            if (measurable.isEmpty()) return@withContext emptyMap()

            val gate = Semaphore(maxConcurrent)
            coroutineScope {
                measurable.map { profile ->
                    async {
                        profile.id to gate.withPermit { probeOne(profile) }
                    }
                }.awaitAll().toMap()
            }
        }

    private fun probeOne(profile: EndpointProfile): EndpointMeasurement {
        val startedAt = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(profile.host, profile.port), connectTimeoutMillis)
                val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L
                EndpointMeasurement.success(elapsedMillis, now())
            }
        } catch (unknown: UnknownHostException) {
            // A name that does not resolve is a real result, not an error to hide.
            EndpointMeasurement.unreachable(now())
        } catch (refused: IOException) {
            EndpointMeasurement.unreachable(now())
        } catch (timeout: java.net.SocketTimeoutException) {
            EndpointMeasurement.unreachable(now())
        } catch (security: SecurityException) {
            EndpointMeasurement.unreachable(now())
        }
    }
}
