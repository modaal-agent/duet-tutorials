// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Tutorial 8's receipt as a test: no recording on the tree carries a string
 * the resources own. A recording is a reducer's state, actions and effects;
 * the resources are the screens' copy in the source language. A reducer that
 * writes a display string into state puts it into its recording, and this
 * test names the fixture and the string. The scenario's own text — the
 * description, the step labels — is left out of the walk. The module's tests
 * run from src-kmp/subtrees/root/logic, so both paths climb to the tree.
 */
class RecordingsHoldNoDisplayTextTest {
  private val fixtures = File("../../../../parity/fixtures")
  private val resources = File("../../../app/src/main/res/values/strings.xml")

  @Test
  fun noRecordingCarriesAStringTheResourcesOwn() {
    val copy = displayText(resources)
    assertTrue(copy.size > 50, "the resources hold the app's copy (${copy.size} values)")
    val files = fixtures.listFiles { file -> file.name.endsWith(".fixture.json") }!!.sorted()
    assertTrue(files.size > 70, "the tree's recordings are on disk (${files.size} files)")

    val leaks = mutableListOf<String>()
    for (file in files) {
      val recorded = mutableListOf<String>()
      val document = Json.parseToJsonElement(file.readText()).jsonObject
      for (field in RECORDED) document[field]?.let { collect(it, recorded) }
      for (step in document.getValue("steps").jsonArray) {
        for (field in RECORDED) step.jsonObject[field]?.let { collect(it, recorded) }
      }
      for (value in recorded) if (value in copy) leaks += "${file.name}: \"$value\""
    }
    assertTrue(leaks.isEmpty(), "display text in recordings:\n" + leaks.joinToString("\n"))
  }

  /** Every string value under one element, depth first. */
  private fun collect(element: JsonElement, into: MutableList<String>) {
    when (element) {
      is JsonObject -> element.values.forEach { collect(it, into) }
      is JsonArray -> element.forEach { collect(it, into) }
      is JsonPrimitive -> if (element.isString) into += element.content
    }
  }

  /** The values of every `<string>` and `<plurals>` item, unescaped. */
  private fun displayText(xml: File): Set<String> =
    Regex("<(?:string|item)[^>]*>([^<]*)</")
      .findAll(xml.readText())
      .map { it.groupValues[1].replace("\\'", "'") }
      .toSet()

  private companion object {
    /** The fields that carry the reducer's values, at the top of a fixture and on each step. */
    val RECORDED = listOf("initialState", "initialStates", "action", "expectedState", "expectedEffects")
  }
}
