// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.theming

import dev.modaal.duet.services.theming.ResolvedAppearance
import kotlin.math.pow

/**
 * The WCAG 2 contrast ratio between two opaque colours in `0xAARRGGBB`: 1.0
 * for equal colours, 21.0 for black on white. Level AA asks 4.5 of body text,
 * level AAA asks 7.
 */
fun contrastRatio(a: Long, b: Long): Double {
  val la = relativeLuminance(a)
  val lb = relativeLuminance(b)
  return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

private fun relativeLuminance(argb: Long): Double {
  fun channel(shift: Int): Double {
    val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
  }
  return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** A label's contrast against a surface under this theme, at an appearance. */
fun Theme.contrast(label: SemanticColor, surface: SemanticColor, appearance: ResolvedAppearance): Double =
  contrastRatio(color(label).value(appearance), color(surface).value(appearance))
