// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.serialization.CanonicalSerializers
import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.home.HomePresentationSerializer
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.main.MainTabSerializer
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.upgrade.UpgradeStep
import dev.modaal.foyer.upgrade.UpgradeStepSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The route spine: each level's route sliver and nothing else, the value
// that survives process death. No domain state travels here: the items
// reload, the session and the entitlement arrive through the streams, and
// each store is rebuilt with its sliver as initial state. Encoded once, by
// the one function below; decoded tolerantly, so a stale or foreign payload
// restores nothing and the app starts fresh.

/** The profile tree's depth, as one value. */
@Serializable(with = ProfilePathSerializer::class)
sealed interface ProfilePath {
  @Serializable @SerialName("account") data object Account : ProfilePath

  @Serializable @SerialName("editName") data object EditName : ProfilePath
}

@Serializable
data class RouteSpine(
  val phase: RootPhase = RootPhase.Splash,
  val activeTab: @Serializable(with = MainTabSerializer::class) MainTab? = null,
  val profilePath: @Serializable(with = ProfilePathSerializer::class) ProfilePath? = null,
  val homePresented: @Serializable(with = HomePresentationSerializer::class) HomePresentation? = null,
  val upgradeStep: @Serializable(with = UpgradeStepSerializer::class) UpgradeStep? = null,
  val onboardingPage: OnboardingPage? = null,
)

fun encodeRouteSpine(spine: RouteSpine): String =
  CanonicalSerializers.json.encodeToString(RouteSpine.serializer(), spine)

/** The spine, or null for text this version does not read. A saved string never fails a launch. */
fun decodeRouteSpine(text: String?): RouteSpine? =
  text?.let { runCatching { CanonicalSerializers.json.decodeFromString(RouteSpine.serializer(), it) }.getOrNull() }
