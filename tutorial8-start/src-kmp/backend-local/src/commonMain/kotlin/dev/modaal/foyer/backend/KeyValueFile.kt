// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

/**
 * Where the backend keeps its one document. Two plain functions, so each app
 * hands the backend a file it owns and a test hands it memory.
 */
interface KeyValueFile {
  /** The document's text, or null when nothing was written yet. */
  fun read(): String?

  fun write(text: String)
}

/** A file on disk at `path`, as UTF-8 text. One implementation per platform. */
class JsonFile(private val path: String) : KeyValueFile {
  override fun read(): String? = readFileText(path)

  override fun write(text: String) = writeFileText(path, text)
}

/** The document held in memory: what the test suites and the composition specs hand the backend. */
class MemoryFile(private var text: String? = null) : KeyValueFile {
  override fun read(): String? = text

  override fun write(text: String) {
    this.text = text
  }
}

internal expect fun readFileText(path: String): String?

internal expect fun writeFileText(path: String, text: String)
