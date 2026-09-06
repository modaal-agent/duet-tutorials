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

// The four ports, as interfaces: every feature that reaches the outside
// world depends on this module.
include(":ports")
// Feature modules live under subtrees/<feature>/logic and are included here
// one line per feature.
include(":subtrees:splash:logic")
include(":subtrees:signin:logic")
include(":subtrees:root:logic")
include(":subtrees:main:logic")
include(":subtrees:home:logic")
include(":subtrees:profile:logic")
include(":subtrees:account:logic")
include(":subtrees:editname:logic")
// The replay-protocol endpoint `tools/duet protocol-run` drives.
include(":replay-runner")
// The Apple boundary: one Kotlin/Native framework over the kernel and every
// feature module, consumed by the iOS app's shells package and by the
// boundary replay suite, both through scripts/assemble_kit.sh.
include(":apple-umbrella")
// The Android app: the Compose shells and the Activity.
include(":app")
