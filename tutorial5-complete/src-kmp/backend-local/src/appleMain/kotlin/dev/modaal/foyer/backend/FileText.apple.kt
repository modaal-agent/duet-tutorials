// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

// Foundation's file calls take an NSError out-parameter, which is what the
// opt-in below is for; a failed read is a missing document, a failed write
// is dropped, and both leave the record as it was.

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFileText(path: String): String? =
  NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

@OptIn(ExperimentalForeignApi::class)
internal actual fun writeFileText(path: String, text: String) {
  @Suppress("CAST_NEVER_SUCCEEDS")
  (text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
}
