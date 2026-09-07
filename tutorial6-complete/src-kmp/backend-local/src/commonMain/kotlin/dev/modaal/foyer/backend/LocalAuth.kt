// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.duet.kernel.KernelClock
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.Session
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.ports.defaultDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object LocalAuthConfig {
  /** How long a guest session lasts. Shorten it to watch the expiry raise the gate. */
  const val GUEST_SESSION_MINUTES = 30L
}

/**
 * The auth port, on device. Any non-empty email and any guest sign in; the
 * session is persisted, so a relaunch starts signed in; a guest session
 * expires after [LocalAuthConfig.GUEST_SESSION_MINUTES], measured on the
 * clock seam from sign-in and again from each launch. `sessions` is sticky:
 * the session worker adopted at the root mount sees the current value first.
 */
class LocalAuth(
  private val storage: LocalStorage,
  private val clock: KernelClock,
  private val scope: CoroutineScope,
) : AuthPort {
  private val mutableSessions = MutableStateFlow(storage.record.currentSession())
  override val sessions: StateFlow<Session> = mutableSessions.asStateFlow()

  private var expiry: Job? = null

  init {
    if (storage.record.session?.guest == true) armExpiry()
  }

  override fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit) {
    if (provider is SignInProvider.Email && provider.address.isBlank()) {
      onOutcome(SignInOutcome.Failed("Enter an email address."))
      return
    }
    val saved = storage.record.displayName
    val record = SessionRecord(saved ?: defaultDisplayName(provider), provider == SignInProvider.Guest)
    storage.update { it.copy(session = record) }
    mutableSessions.value = storage.record.currentSession()
    if (record.guest) armExpiry()
    onOutcome(SignInOutcome.SignedIn(saved, storage.record.hasOnboarded))
  }

  override fun signOut(onDone: () -> Unit) {
    endSession()
    onDone()
  }

  /** The account port saved a new name; a live session carries it from now on. */
  internal fun sessionRenamed(displayName: String) {
    val session = storage.record.session ?: return
    storage.update { it.copy(session = session.copy(displayName = displayName)) }
    mutableSessions.value = storage.record.currentSession()
  }

  /** The account port marked the account onboarded; a live session carries the fact from now on. */
  internal fun sessionOnboarded() {
    if (storage.record.session == null) return
    mutableSessions.value = storage.record.currentSession()
  }

  private fun armExpiry() {
    expiry?.cancel()
    expiry =
      scope.launch {
        clock.sleep(LocalAuthConfig.GUEST_SESSION_MINUTES * 60 * 1_000_000_000L)
        endSession()
      }
  }

  private fun endSession() {
    expiry?.cancel()
    expiry = null
    storage.update { it.copy(session = null) }
    mutableSessions.value = Session.SignedOut
  }
}

/** The stream's value for the document as it is now. */
private fun LocalRecord.currentSession(): Session =
  session?.let { Session.SignedIn(it.displayName, hasOnboarded) } ?: Session.SignedOut
