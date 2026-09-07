// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The persisted session: who is signed in, and whether the session expires. */
@Serializable data class SessionRecord(val displayName: String, val guest: Boolean)

/**
 * The one document the backend persists. A field per thing that survives a
 * relaunch: the session, the saved display name, the purchased plan.
 */
@Serializable
data class LocalRecord(
  val session: SessionRecord? = null,
  val displayName: String? = null,
  val plan: @Serializable(with = PlanSerializer::class) Plan? = null,
)

/**
 * Reads the document once at construction and writes it whole on every
 * update. Unreadable text (a first launch, a document from another version)
 * starts from the empty record.
 */
class LocalStorage(private val file: KeyValueFile) {
  var record: LocalRecord = load()
    private set

  fun update(transform: (LocalRecord) -> LocalRecord) {
    record = transform(record)
    file.write(json.encodeToString(LocalRecord.serializer(), record))
  }

  private fun load(): LocalRecord =
    file.read()?.let { text ->
      runCatching { json.decodeFromString(LocalRecord.serializer(), text) }.getOrNull()
    } ?: LocalRecord()

  private companion object {
    val json = Json { ignoreUnknownKeys = true }
  }
}
