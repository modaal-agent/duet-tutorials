// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.theming

import dev.modaal.duet.services.theming.ResolvedAppearance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The property the second theme exists for, pinned: every label reads at
 * WCAG level AAA on every surface in both appearances, the card border is
 * opaque ink, and the type scale is the main theme's.
 */
class HighContrastThemeTest {
  private val labels = listOf(SemanticColor.labelPrimary, SemanticColor.labelSecondary, SemanticColor.labelLocked)
  private val surfaces = listOf(SemanticColor.surface, SemanticColor.cardSurface, SemanticColor.lockedSurface)

  @Test
  fun everyLabelReadsAtAaaOnEveryCard() {
    for (appearance in ResolvedAppearance.entries) {
      for (surface in surfaces) {
        for (label in labels) {
          val ratio = HighContrastTheme.contrast(label, surface, appearance)
          assertTrue(ratio >= 7.0, "$label on $surface at $appearance: ${"%.2f".format(ratio)}")
        }
      }
    }
  }

  @Test
  fun theCardBorderIsOpaqueInk() {
    for (appearance in ResolvedAppearance.entries) {
      val border = HighContrastTheme.color(SemanticColor.cardBorder).value(appearance)
      assertEquals(0xFFL, (border shr 24) and 0xFF, "the border is opaque at $appearance")
      assertEquals(HighContrastTheme.color(SemanticColor.labelPrimary).value(appearance), border)
    }
  }

  @Test
  fun theTypeStylesAreTheMainThemes() {
    for (token in SemanticFont.entries) {
      assertEquals(MainTheme.font(token).sizeSp, HighContrastTheme.font(token).sizeSp, "$token")
      assertEquals(MainTheme.font(token).weight, HighContrastTheme.font(token).weight, "$token")
    }
  }
}
