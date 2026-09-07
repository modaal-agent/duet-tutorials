// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.account.AccountActionSerializer
import dev.modaal.foyer.account.AccountDelegateEvent
import dev.modaal.foyer.account.AccountDelegateEventSerializer
import dev.modaal.foyer.account.AccountEffectPayload
import dev.modaal.foyer.account.AccountEffectPayloadSerializer
import dev.modaal.foyer.account.AccountEvents
import dev.modaal.foyer.account.AccountState
import dev.modaal.foyer.account.accountReducer
import dev.modaal.foyer.main.MainAction
import dev.modaal.foyer.main.MainActionSerializer
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.main.MainDelegateEventSerializer
import dev.modaal.foyer.main.MainEffectPayload
import dev.modaal.foyer.main.MainEffectPayloadSerializer
import dev.modaal.foyer.main.MainState
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.main.mainReducer
import dev.modaal.foyer.onboarding.OnboardingAction
import dev.modaal.foyer.onboarding.OnboardingActionSerializer
import dev.modaal.foyer.onboarding.OnboardingDelegateEvent
import dev.modaal.foyer.onboarding.OnboardingDelegateEventSerializer
import dev.modaal.foyer.onboarding.OnboardingEffectPayload
import dev.modaal.foyer.onboarding.OnboardingEffectPayloadSerializer
import dev.modaal.foyer.onboarding.OnboardingEvents
import dev.modaal.foyer.onboarding.OnboardingState
import dev.modaal.foyer.onboarding.onboardingReducer
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.preferences.PreferencesDelegateEvent
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.profile.ProfileAction
import dev.modaal.foyer.profile.ProfileActionSerializer
import dev.modaal.foyer.profile.ProfileRoute
import dev.modaal.foyer.profile.ProfileDelegateEvent
import dev.modaal.foyer.profile.ProfileDelegateEventSerializer
import dev.modaal.foyer.profile.ProfileEffectPayload
import dev.modaal.foyer.profile.ProfileEffectPayloadSerializer
import dev.modaal.foyer.profile.ProfileState
import dev.modaal.foyer.profile.profileReducer
import dev.modaal.foyer.signin.SignInAction
import dev.modaal.foyer.signin.SignInActionSerializer
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.signin.SignInDelegateEventSerializer
import dev.modaal.foyer.signin.SignInEffectPayload
import dev.modaal.foyer.signin.SignInEffectPayloadSerializer
import dev.modaal.foyer.signin.SignInEvents
import dev.modaal.foyer.signin.SignInState
import dev.modaal.foyer.signin.signInReducer
import dev.modaal.foyer.splash.SplashAction
import dev.modaal.foyer.splash.SplashActionSerializer
import dev.modaal.foyer.splash.SplashCompletionPath
import dev.modaal.foyer.splash.SplashDelegateEvent
import dev.modaal.foyer.splash.SplashDelegateEventSerializer
import dev.modaal.foyer.splash.SplashEffectPayload
import dev.modaal.foyer.splash.SplashEffectPayloadSerializer
import dev.modaal.foyer.splash.SplashState
import dev.modaal.foyer.splash.splashReducer
import kotlin.test.Test

/**
 * The chain scenarios that end at the root: the splash seam, the two gate
 * seams, and the sign-out climbing four levels. A chain pins a composition seam
 * across nodes: each `hop` is the delegate-to-action forwarding the shells
 * perform in production, re-derived at verify time from the replayed
 * payload. Chains carry no state, so the leaf fixtures keep pinning each
 * node's state on its own.
 */
class RootChainsTest {

  private val root =
    ChainNode(
      "root",
      RootState(auth = AuthSnapshot.SignedOut),
      RootState.serializer(),
      RootActionSerializer,
      RootEffectPayloadSerializer,
      ::rootReducer)

  private val splash =
    ChainNode(
      "splash",
      SplashState(isArmed = true),
      SplashState.serializer(),
      SplashActionSerializer,
      SplashEffectPayloadSerializer,
      ::splashReducer)

  @Test
  fun theSplashSeam() {
    val chain =
      chainScenario(
        chain = "root-splash",
        fixture = "chain-root-splash",
        description =
          "The splash's Completed crosses into the root as Splash(event); a " +
            "signed-out root raises the gate.",
        source =
          "src-kmp/subtrees/root/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/root/RootChainsTest.kt",
      ) {
        whenAction(splash, "the ceremony ends", SplashAction.CeremonyFinished)
        thenEffects(splash, "exactly the Completed delegate, by the ceremony path") {
          it ==
            effectsOf<SplashEffectPayload>(
              Effect.Run(
                SplashEffectPayload.NotifyHost(
                  SplashDelegateEvent.Completed(SplashCompletionPath.Ceremony))))
        }
        hop(
          "the splash's delegate is the root's action",
          from = splash,
          to = root,
          delegateSerializer = SplashDelegateEventSerializer,
        ) { event ->
          RootAction.Splash(event)
        }
        then(root, "the gate is up") { it.phase == RootPhase.SignIn }
        thenEffects(root, "the launch event, by the ceremony path") {
          it ==
            effectsOf<RootEffectPayload>(
              Effect.Run(
                RootEffectPayload.Track(RootEvents.splashCompleted(SplashCompletionPath.Ceremony))))
        }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }

  @Test
  fun theGateSeam() {
    val signIn =
      ChainNode(
        "signin",
        SignInState(isSigningIn = true, pending = SignInProvider.Email("ann@example.com")),
        SignInState.serializer(),
        SignInActionSerializer,
        SignInEffectPayloadSerializer,
        ::signInReducer)
    val gatedRoot =
      ChainNode(
        "root",
        RootState(phase = RootPhase.SignIn, auth = AuthSnapshot.SignedOut),
        RootState.serializer(),
        RootActionSerializer,
        RootEffectPayloadSerializer,
        ::rootReducer)
    val chain =
      chainScenario(
        chain = "root-signin",
        fixture = "chain-root-signin",
        description =
          "The gate's Completed crosses into the root as SignIn(event); the root " +
            "signs the session in and mounts main.",
        source =
          "src-kmp/subtrees/root/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/root/RootChainsTest.kt",
      ) {
        whenAction(
          signIn,
          "the auth port signs the account in",
          SignInAction.SignInFinished(SignInOutcome.SignedIn("Ann", hasOnboarded = true)))
        thenEffects(signIn, "the sign-in event, then the Completed delegate") {
          it ==
            effectsOf<SignInEffectPayload>(
              Effect.Run(
                SignInEffectPayload.Track(
                  SignInEvents.signedIn(SignInProvider.Email("ann@example.com")))),
              Effect.Run(SignInEffectPayload.NotifyHost(SignInDelegateEvent.Completed("Ann", true))))
        }
        hop(
          "the gate's delegate is the root's action",
          from = signIn,
          to = gatedRoot,
          delegateSerializer = SignInDelegateEventSerializer,
        ) { event ->
          RootAction.SignIn(event)
        }
        then(gatedRoot, "main is up, the session signed in") {
          it.phase == RootPhase.Main && it.auth == AuthSnapshot.SignedIn("Ann", true)
        }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }

  @Test
  fun theSignOutClimbsFourLevels() {
    val account =
      ChainNode(
        "account",
        AccountState(displayName = "Ann", isSigningOut = true),
        AccountState.serializer(),
        AccountActionSerializer,
        AccountEffectPayloadSerializer,
        ::accountReducer)
    val profile =
      ChainNode(
        "profile",
        ProfileState(displayName = "Ann", child = ProfileRoute.Account),
        ProfileState.serializer(),
        ProfileActionSerializer,
        ProfileEffectPayloadSerializer,
        ::profileReducer)
    val main =
      ChainNode(
        "main",
        MainState(activeTab = MainTab.Profile),
        MainState.serializer(),
        MainActionSerializer,
        MainEffectPayloadSerializer,
        ::mainReducer)
    val signedInRoot =
      ChainNode(
        "root",
        RootState(phase = RootPhase.Main, auth = AuthSnapshot.SignedIn("Ann", true)),
        RootState.serializer(),
        RootActionSerializer,
        RootEffectPayloadSerializer,
        ::rootReducer)
    val chain =
      chainScenario(
        chain = "main-signout",
        fixture = "chain-main-signout",
        description =
          "A confirmed sign-out climbs from the account screen through the profile " +
            "tab and the main level to the root, which raises the gate. Each level " +
            "relays the request unchanged.",
        source =
          "src-kmp/subtrees/root/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/root/RootChainsTest.kt",
      ) {
        whenAction(account, "the auth port confirms the sign-out", AccountAction.SignedOut)
        thenEffects(account, "the sign-out event, then the SignOutRequested delegate") {
          it ==
            effectsOf<AccountEffectPayload>(
              Effect.Run(AccountEffectPayload.Track(AccountEvents.signedOut)),
              Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.SignOutRequested)))
        }
        hop(
          "account to profile",
          from = account,
          to = profile,
          delegateSerializer = AccountDelegateEventSerializer,
        ) { event ->
          ProfileAction.Account(event)
        }
        thenEffects(profile, "relayed") {
          it ==
            effectsOf<ProfileEffectPayload>(
              Effect.Run(ProfileEffectPayload.NotifyHost(ProfileDelegateEvent.SignOutRequested)))
        }
        hop(
          "profile to main",
          from = profile,
          to = main,
          delegateSerializer = ProfileDelegateEventSerializer,
        ) { event ->
          MainAction.Profile(event)
        }
        thenEffects(main, "relayed") {
          it ==
            effectsOf<MainEffectPayload>(
              Effect.Run(MainEffectPayload.NotifyHost(MainDelegateEvent.SignOutRequested)))
        }
        hop(
          "main to root",
          from = main,
          to = signedInRoot,
          delegateSerializer = MainDelegateEventSerializer,
        ) { event ->
          RootAction.Main(event)
        }
        then(signedInRoot, "the gate is up, the session signed out") {
          it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
        }
        thenEffects(signedInRoot, "nothing climbs further") { it.isEmpty() }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }

  @Test
  fun theOnboardingGateSeam() {
    val onboarding =
      ChainNode(
        "onboarding",
        OnboardingState(page = OnboardingPage.Preferences, name = "Ann"),
        OnboardingState.serializer(),
        OnboardingActionSerializer,
        OnboardingEffectPayloadSerializer,
        ::onboardingReducer)
    val onboardingRoot =
      ChainNode(
        "root",
        RootState(phase = RootPhase.Onboarding, auth = AuthSnapshot.SignedIn("ann", false)),
        RootState.serializer(),
        RootActionSerializer,
        RootEffectPayloadSerializer,
        ::rootReducer)
    val chain =
      chainScenario(
        chain = "root-onboarding",
        fixture = "chain-root-onboarding",
        description =
          "The onboarding gate's Completed crosses into the root as Onboarding(event); " +
            "the root persists the answers through the account port and mounts main.",
        source =
          "src-kmp/subtrees/root/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/root/RootChainsTest.kt",
      ) {
        whenAction(
          onboarding,
          "the last step continues",
          OnboardingAction.Preferences(PreferencesDelegateEvent.Continued(listOf("digest"))))
        thenEffects(onboarding, "the event, then the Completed delegate") {
          it ==
            effectsOf<OnboardingEffectPayload>(
              Effect.Run(
                OnboardingEffectPayload.Track(OnboardingEvents.completed(preferenceCount = 1))),
              Effect.Run(
                OnboardingEffectPayload.NotifyHost(
                  OnboardingDelegateEvent.Completed("Ann", listOf("digest")))))
        }
        hop(
          "the gate's delegate is the root's action",
          from = onboarding,
          to = onboardingRoot,
          delegateSerializer = OnboardingDelegateEventSerializer,
        ) { event ->
          RootAction.Onboarding(event)
        }
        then(onboardingRoot, "main is up, the snapshot onboarded") {
          it.phase == RootPhase.Main && it.auth == AuthSnapshot.SignedIn("Ann", true)
        }
        thenEffects(onboardingRoot, "exactly the onboarding write") {
          it ==
            effectsOf<RootEffectPayload>(
              Effect.Run(RootEffectPayload.CompleteOnboarding("Ann", listOf("digest"))))
        }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }
}
