package com.kx.vpn.domain.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UP is only a backend fact, never a claim of handshake, Internet or protection. */
enum class SessionPhase { Unconfigured, Disconnected, Connecting, Up, Disconnecting, Failed }
enum class SessionProblem { None, Configuration, PermissionDenied, Backend, Storage, CleanupRequired }
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
        check(mutable.value.problem != SessionProblem.CleanupRequired)
        check(mutable.value.phase !in setOf(SessionPhase.Up, SessionPhase.Connecting, SessionPhase.Disconnecting))
        mutable.value = SessionSnapshot(SessionPhase.Disconnected)
    }

    fun permissionDenied() {
        if (mutable.value.problem != SessionProblem.CleanupRequired &&
            mutable.value.phase in setOf(SessionPhase.Disconnected, SessionPhase.Failed)) {
            mutable.value = SessionSnapshot(SessionPhase.Disconnected, SessionProblem.PermissionDenied)
        }
    }

    suspend fun connect() = lock.withLock {
        if (mutable.value.problem == SessionProblem.CleanupRequired ||
            mutable.value.phase !in setOf(SessionPhase.Disconnected, SessionPhase.Failed)) return@withLock
        mutable.value = SessionSnapshot(SessionPhase.Connecting)
        val generation = synchronized(callbackLock) { downGeneration }
        try {
            val up = driver.start()
            val stillUp = synchronized(callbackLock) {
                if (up && generation == downGeneration) {
                    mutable.value = SessionSnapshot(SessionPhase.Up)
                    true
                } else false
            }
            if (!stillUp) failAfterCleanup()
        } catch (cancelled: CancellationException) {
            // The native call may already have created the tunnel when withContext
            // resumes into a cancelled owner. Changing UI state is not teardown.
            failAfterCleanup()
            throw cancelled
        } catch (_: Exception) {
            failAfterCleanup()
        }
    }

    /** Best effort with a cooperative deadline; not a hard bound on blocking JNI. */
    private suspend fun failAfterCleanup() {
        val stopped = withContext(NonCancellable) {
            withTimeoutOrNull(5_000L) {
                try { driver.stop() } catch (_: Exception) { false }
            } == true
        }
        mutable.value = SessionSnapshot(SessionPhase.Failed,
            if (stopped) SessionProblem.Backend else SessionProblem.CleanupRequired)
    }

    suspend fun disconnect(preserveFailure: Boolean = false) = lock.withLock {
        if (mutable.value.phase == SessionPhase.Unconfigured || mutable.value.phase == SessionPhase.Disconnected) return@withLock
        val previous = mutable.value
        val retainError = preserveFailure && previous.phase == SessionPhase.Failed
        if (!retainError) mutable.value = SessionSnapshot(SessionPhase.Disconnecting)
        try {
            val stopped = driver.stop()
            mutable.value = when {
                !stopped -> SessionSnapshot(SessionPhase.Failed, SessionProblem.CleanupRequired)
                retainError -> previous.copy(problem = SessionProblem.Backend)
                else -> SessionSnapshot(SessionPhase.Disconnected)
            }
        } catch (cancelled: CancellationException) {
            failAfterCleanup()
            throw cancelled
        } catch (_: Exception) {
            mutable.value = SessionSnapshot(SessionPhase.Failed, SessionProblem.CleanupRequired)
        }
    }

    /** Library DOWN callback, including Android revocation/destruction. */
    fun backendDown() = synchronized(callbackLock) {
        downGeneration++
        if (mutable.value.phase == SessionPhase.Up) mutable.value = SessionSnapshot(SessionPhase.Disconnected)
    }
}
