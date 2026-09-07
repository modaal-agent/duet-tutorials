// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.OnboardingPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun onboardingEffectHandler(
  environment: OnboardingEnvironment,
): (OnboardingEffectPayload) -> Flow<OnboardingAction> = { payload ->
  flow {
    when (payload) {
      is OnboardingEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store a shell hosts; `page` is the restored route sliver, or the first page. */
fun makeOnboardingStore(
  page: OnboardingPage,
  environment: OnboardingEnvironment,
  scope: CoroutineScope,
): Store<OnboardingState, OnboardingAction, OnboardingEffectPayload> =
  Store(
    initialState = OnboardingState(page = page),
    reducer = ::onboardingReducer,
    handler = onboardingEffectHandler(environment),
    scope = scope,
  )

fun onboardingStateFlow(
  store: Store<OnboardingState, OnboardingAction, OnboardingEffectPayload>,
): StateFlow<OnboardingState> = store.state
