package com.kx.vpn.domain.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UP is only a backend fact, never a claim of handshake, Internet or protection. */
enum class SessionPhase { Unconfigured, Disconnected, Connecting, Up, Disconnecting, Failed }
enum class SessionProblem { None, Configuration, PermissionDenied, Backend, Storage }
data class SessionSnapshot(
    val phase: SessionPhase = SessionPhase.Unconfigured,
    val problem: SessionProblem = SessionProblem.None,
)

interface TunnelDriver {
    suspend fun start(): Boolean
    suspend fun stop(): Boolean
}

/** Serialized commands. Errors contain only categories, never backend/config text. */
class SessionController(private val driver: TunnelDriver) {
    private val lock = Mutex()
    private val callbackLock = Any()
    private var downGeneration = 0L
    private val mutable = MutableStateFlow(SessionSnapshot())
    val state: StateFlow<SessionSnapshot> = mutable.asStateFlow()

    suspend fun configured() = lock.withLock {
        check(mutable.value.phase !in setOf(SessionPhase.Up, SessionPhase.Connecting, SessionPhase.Disconnecting))
        mutable.value = SessionSnapshot(SessionPhase.Disconnected)
    }

    fun permissionDenied() {
        if (mutable.value.phase in setOf(SessionPhase.Disconnected, SessionPhase.Failed)) {
            mutable.value = SessionSnapshot(SessionPhase.Disconnected, SessionProblem.PermissionDenied)
        }
    }

    suspend fun connect() = lock.withLock {
        if (mutable.value.phase !in setOf(SessionPhase.Disconnected, SessionPhase.Failed)) return@withLock
        mutable.value = SessionSnapshot(SessionPhase.Connecting)
        val generation = synchronized(callbackLock) { downGeneration }
        try {
            val up = driver.start()
            synchronized(callbackLock) {
                mutable.value = if (up && generation == downGeneration) SessionSnapshot(SessionPhase.Up)
                else SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
            }
        } catch (cancelled: CancellationException) {
            // Do not report a connection after cancelling the command's owner.
            mutable.value = SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
            throw cancelled
        } catch (_: Exception) {
            mutable.value = SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
        }
    }

    suspend fun disconnect() = lock.withLock {
        if (mutable.value.phase == SessionPhase.Unconfigured || mutable.value.phase == SessionPhase.Disconnected) return@withLock
        mutable.value = SessionSnapshot(SessionPhase.Disconnecting)
        try {
            mutable.value = if (driver.stop()) SessionSnapshot(SessionPhase.Disconnected)
            else SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
        } catch (cancelled: CancellationException) {
            mutable.value = SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
            throw cancelled
        } catch (_: Exception) {
            mutable.value = SessionSnapshot(SessionPhase.Failed, SessionProblem.Backend)
        }
    }

    /** Library DOWN callback, including Android revocation/destruction. */
    fun backendDown() = synchronized(callbackLock) {
        downGeneration++
        if (mutable.value.phase == SessionPhase.Up) mutable.value = SessionSnapshot(SessionPhase.Disconnected)
    }
}
