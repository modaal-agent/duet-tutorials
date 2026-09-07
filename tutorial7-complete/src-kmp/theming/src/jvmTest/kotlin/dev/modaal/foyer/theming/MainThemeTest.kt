// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.theming

import dev.modaal.duet.services.theming.ResolvedAppearance
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The main theme read the way a screen reads it: every token has a value in
 * both appearances, and every label meets WCAG level AA on every surface it
 * can sit on. The values are the config's, so a value edit that drops a label
 * under 4.5:1 fails here before it ships.
 */
class MainThemeTest {
  private val labels = listOf(SemanticColor.labelPrimary, SemanticColor.labelSecondary)
  private val surfaces = listOf(SemanticColor.surface, SemanticColor.cardSurface, SemanticColor.lockedSurface)

  @Test
  fun everyTokenResolvesInBothAppearances() {
    for (appearance in ResolvedAppearance.entries) {
      for (token in SemanticColor.entries) {
        assertNotEquals(0L, MainTheme.color(token).value(appearance), "$token at $appearance")
      }
    }
    for (token in SemanticFont.entries) assertTrue(MainTheme.font(token).sizeSp > 0.0, "$token")
  }

  @Test
  fun everyLabelReadsAtAaOnEveryCard() {
    for (appearance in ResolvedAppearance.entries) {
      for (surface in surfaces) {
        for (label in labels) {
          val ratio = MainTheme.contrast(label, surface, appearance)
          assertTrue(ratio >= 4.5, "$label on $surface at $appearance: ${"%.2f".format(ratio)}")
        }
      }
      val locked = MainTheme.contrast(SemanticColor.labelLocked, SemanticColor.lockedSurface, appearance)
      assertTrue(locked >= 4.5, "labelLocked on lockedSurface at $appearance: ${"%.2f".format(locked)}")
    }
  }
}
