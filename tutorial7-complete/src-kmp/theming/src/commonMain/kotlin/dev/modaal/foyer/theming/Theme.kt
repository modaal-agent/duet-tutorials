// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.theming

import dev.modaal.duet.services.theming.ColorToken
import dev.modaal.duet.services.theming.FontToken

/**
 * A theme: the value every token of the vocabulary takes. A screen names a
 * token, the theme in effect names its value, and the screen never holds a
 * colour literal. Both themes read `MainPalette`, the table generated from
 * parity/design-tokens.yaml: the main theme entry for entry, the high-contrast
 * theme through a mapping, so the config stays the one place a value is
 * written.
 */
interface Theme {
  fun color(token: SemanticColor): ColorToken

  fun font(token: SemanticFont): FontToken
}

/** The main theme: every token at the value the config states for it. */
object MainTheme : Theme {
  override fun color(token: SemanticColor): ColorToken = MainPalette.color(token)

  override fun font(token: SemanticFont): FontToken = MainPalette.font(token)
}

/**
 * The high-contrast theme, in effect while the system's contrast setting is
 * raised: every surface collapses onto the page and every label and the card
 * border onto the primary label, so a card is the page's colour with a solid
 * line of ink around it and nothing on it is grey. The type styles are the
 * main theme's. The mapping is exhaustive: a token added to the config is a
 * compile error here until it says where the token maps.
 */
object HighContrastTheme : Theme {
  override fun color(token: SemanticColor): ColorToken = MainPalette.color(token.highContrastSource)

  override fun font(token: SemanticFont): FontToken = MainPalette.font(token)
}

/** The main-palette entry a token reads in the high-contrast theme. */
internal val SemanticColor.highContrastSource: SemanticColor
  get() =
    when (this) {
      SemanticColor.surface,
      SemanticColor.cardSurface,
      SemanticColor.lockedSurface -> SemanticColor.surface
      SemanticColor.labelPrimary,
      SemanticColor.labelSecondary,
      SemanticColor.labelLocked,
      SemanticColor.cardBorder -> SemanticColor.labelPrimary
    }
