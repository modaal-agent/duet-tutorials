// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import dev.modaal.duet.shells.RetainedRoot
import dev.modaal.foyer.backend.JsonFile
import dev.modaal.foyer.backend.KeyValueFile
import dev.modaal.foyer.ports.parseDeepLink
import dev.modaal.foyer.root.RootAction
import dev.modaal.foyer.root.decodeRouteSpine
import dev.modaal.foyer.root.encodeRouteSpine
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
 * store still running; finishing the Activity is the one teardown. Process
 * death is the other case: the route spine rides the saved-instance Bundle
 * out and back, and the tree is rebuilt from it. A deep link, at launch or
 * while running, is parsed here and handed to the root as one action.
 */
class MainActivity : ComponentActivity() {
  private lateinit var retained: RetainedRoot<RootMount>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val dependency = ActivityRootDependency(filesDir)
    // Null on a fresh start and on a rotation with the mount retained;
    // the saved spine after process death. A stale payload decodes to null.
    val restored = decodeRouteSpine(savedInstanceState?.getString(SPINE_KEY))
    retained =
      instanceKeeper().getOrCreate {
        RetainedRoot(Dispatchers.Main.immediate, RootMount::teardown) { scope ->
          RootBuilder(dependency).buildRoot(scope, restored)
        }
      }
    if (savedInstanceState == null) openLink(intent)

    setContent { AppRoot(retained.component) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    openLink(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(SPINE_KEY, encodeRouteSpine(retained.component.routeSpine()))
  }

  /** The intent's URL as a link, if it is one the app answers; the root routes it from there. */
  private fun openLink(intent: Intent?) {
    val link = intent?.dataString?.let(::parseDeepLink) ?: return
    retained.component.store.send(RootAction.DeepLink(link))
  }

  private companion object {
    const val SPINE_KEY = "dev.modaal.foyer.routeSpine"
  }
}
