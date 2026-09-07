// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import java.io.File

internal actual fun readFileText(path: String): String? =
  File(path).takeIf { it.isFile }?.readText()

internal actual fun writeFileText(path: String, text: String) {
  File(path).writeText(text)
}
