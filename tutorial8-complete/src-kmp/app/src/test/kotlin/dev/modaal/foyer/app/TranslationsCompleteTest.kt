// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.Element

/**
 * The second language is complete: every name in values/strings.xml has a
 * German entry, no German entry is an orphan, and each pair carries the same
 * placeholders. Gradle runs a module's unit tests from the module directory,
 * so the paths are relative to src-kmp/app. A missing translation is not a
 * build error on Android — the source language fills in — which is why a
 * test holds the two files together.
 */
class TranslationsCompleteTest {
  private val source = entries(File("src/main/res/values/strings.xml"))
  private val german = entries(File("src/main/res/values-de/strings.xml"))

  @Test
  fun everyNameHasAGermanEntryAndNoGermanEntryIsAnOrphan() {
    assertEquals(source.keys, german.keys)
  }

  @Test
  fun everyPairCarriesTheSamePlaceholders() {
    for ((name, value) in source) {
      assertEquals(placeholders(value), placeholders(german.getValue(name)), "placeholders of $name")
    }
  }

  /** name → value for each `<string>`; name/quantity → value for each `<plurals>` item. */
  private fun entries(file: File): Map<String, String> {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val out = linkedMapOf<String, String>()
    val strings = document.getElementsByTagName("string")
    for (i in 0 until strings.length) {
      val string = strings.item(i) as Element
      out[string.getAttribute("name")] = string.textContent
    }
    val plurals = document.getElementsByTagName("plurals")
    for (i in 0 until plurals.length) {
      val plural = plurals.item(i) as Element
      val items = plural.getElementsByTagName("item")
      for (j in 0 until items.length) {
        val item = items.item(j) as Element
        out["${plural.getAttribute("name")}/${item.getAttribute("quantity")}"] = item.textContent
      }
    }
    return out
  }

  /** The positional placeholders of one value: `%1$s`, `%2$d`. */
  private fun placeholders(value: String): Set<String> =
    Regex("""%\d+\$[sd]""").findAll(value).map { it.value }.toSet()
}
