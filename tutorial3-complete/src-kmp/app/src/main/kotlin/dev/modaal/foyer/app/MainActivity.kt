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
import kotlinx.coroutines.Dispatchers

/** The Activity's conformer to the root's Dependency: the platform supplies nothing yet. */
private object ActivityRootDependency : RootDependency

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

    retained =
      instanceKeeper().getOrCreate {
        RetainedRoot(Dispatchers.Main.immediate, RootMount::teardown) { scope ->
          RootBuilder(ActivityRootDependency).buildRoot(scope)
        }
      }

    setContent { AppRoot(retained.component) }
  }
}
