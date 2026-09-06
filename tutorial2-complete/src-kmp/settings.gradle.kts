pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  repositories {
    // The Duet family's Maven host. The content filter keeps Gradle from
    // probing it for anything outside dev.modaal.*.
    maven {
      url = uri("https://modaal-agent.github.io/maven")
      content { includeGroupByRegex("""dev\.modaal(\..*)?""") }
    }
    mavenCentral()
    google()
  }
}

rootProject.name = "foyer-kmp"

// Feature modules live under subtrees/<feature>/logic and are included here
// one line per feature.
include(":subtrees:splash:logic")
// The replay-protocol endpoint `tools/duet protocol-run` drives.
include(":replay-runner")
// The Apple boundary: one Kotlin/Native framework over the kernel and every
// feature module, consumed by the iOS app's shells package and by the
// boundary replay suite, both through scripts/assemble_kit.sh.
include(":apple-umbrella")
// The Android app: the Compose shells and the Activity.
include(":app")
