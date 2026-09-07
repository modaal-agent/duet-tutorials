// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.telemetry

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.encodedName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The app's event taxonomy, read back from the recordings. Every `track`
 * effect a reducer emitted is in a fixture under parity/fixtures in the
 * grammar's wire form; this test decodes each one with the grammar's own
 * serializer and holds the set of vendor-facing names to the seven the app
 * declares. A new emission is a new dashboard name, so it lands here first.
 * Every param key is snake_case and every value a primitive: the privacy
 * rule, checked on the bytes. This suite lives in the grammar module, which
 * no feature depends on for tests, so `record` never runs it and the
 * workflow's `:telemetry:jvmTest` step does; its tests run from
 * src-kmp/telemetry, so the path climbs to the tree.
 */
class TrackedEventsInRecordingsTest {
  private val fixtures = File("../../parity/fixtures")

  @Test
  fun theRecordingsCarryExactlyTheDeclaredEvents() {
    val files = fixtures.listFiles { file -> file.name.endsWith(".fixture.json") }!!.sorted()
    assertTrue(files.size > 70, "the tree's recordings are on disk (${files.size} files)")

    val events = mutableListOf<Pair<String, TrackedEvent>>()
    for (file in files) {
      val document = Json.parseToJsonElement(file.readText()).jsonObject
      for (step in document.getValue("steps").jsonArray) {
        val effects = step.jsonObject["expectedEffects"] ?: continue
        collectTracks(effects) { envelope ->
          events += file.name to Json.decodeFromJsonElement(TrackedEvent.serializer(), envelope)
        }
      }
    }

    assertEquals(DECLARED, events.map { it.second.encodedName() }.toSet(), "the names in the recordings")
    for ((file, event) in events) {
      for (param in event.params) {
        assertTrue(param.key.matches(Regex("[a-z]+(_[a-z]+)*")), "$file: param key ${param.key}")
      }
    }
  }

  /** Every `{"case": "track", "value": {"event": …}}` envelope under one element. */
  private fun collectTracks(element: JsonElement, found: (JsonElement) -> Unit) {
    when (element) {
      is JsonObject -> {
        if (element["case"]?.jsonPrimitive?.content == "track") {
          found(element.getValue("value").jsonObject.getValue("event"))
        } else {
          element.values.forEach { collectTracks(it, found) }
        }
      }
      is JsonArray -> element.forEach { collectTracks(it, found) }
      is JsonPrimitive -> Unit
    }
  }

  private companion object {
    /** The taxonomy: one line per event the app tracks, spelled as a dashboard reads it. */
    val DECLARED =
      setOf(
        "Splash Completed",
        "Session Signed In",
        "Onboarding Completed",
        "Promo Viewed",
        "Upgrade Completed",
        "Name Edited",
        "Session Signed Out",
      )
  }
}
