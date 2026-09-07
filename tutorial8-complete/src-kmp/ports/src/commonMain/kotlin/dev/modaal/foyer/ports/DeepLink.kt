// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// A deep link is the one input the operating system hands the app after
// launch. Each shell parses the URL it received with the function below and
// sends the value to the root; from there the link travels down the tree as
// route state, one level at a time.

/** The two links the app answers. */
@Serializable(with = DeepLinkSerializer::class)
sealed interface DeepLink {
  /** `foyer://upgrade`: open the upgrade flow over whichever tab is active. */
  @Serializable @SerialName("upgrade") data object Upgrade : DeepLink

  /** `foyer://profile/account`: the profile tab with the account screen mounted. */
  @Serializable @SerialName("profileAccount") data object ProfileAccount : DeepLink
}

object DeepLinkConfig {
  /** The URL scheme both apps register. */
  const val SCHEME = "foyer"
}

/**
 * The URL as a link, or null for any URL the app does not answer. Both shells
 * call this, so the two platforms accept exactly the same URLs.
 */
fun parseDeepLink(url: String): DeepLink? {
  val prefix = "${DeepLinkConfig.SCHEME}://"
  if (!url.startsWith(prefix)) return null
  return when (url.removePrefix(prefix).trimEnd('/')) {
    "upgrade" -> DeepLink.Upgrade
    "profile/account" -> DeepLink.ProfileAccount
    else -> null
  }
}
