// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import dev.modaal.duet.shells.RetainedRoot
import dev.modaal.foyer.backend.JsonFile
import dev.modaal.foyer.backend.KeyValueFile
import java.io.File
import kotlinx.coroutines.Dispatchers

/** The Activity's conformer to the root's Dependency: the backend's document, under the app's private files. */
private class ActivityRootDependency(filesDir: File) : RootDependency {
  override val storage: KeyValueFile = JsonFile(File(filesDir, "foyer.json").path)
}

/**
 * The Android edge, and nothing else: the Activity keeps the root mount on a
 * retained scope and renders the Compose tree over it. Rotation recreates
 * the Activity, and `getOrCreate` hands the same mount back with every
 * store still running; finishing the Activity is the one teardown.
 */
class MainActivity : ComponentActivity() {
  private lateinit var retained: RetainedRoot<RootMount>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val dependency = ActivityRootDependency(filesDir)
    retained =
      instanceKeeper().getOrCreate {
        RetainedRoot(Dispatchers.Main.immediate, RootMount::teardown) { scope ->
          RootBuilder(dependency).buildRoot(scope)
        }
      }

    setContent { AppRoot(retained.component) }
  }
}
