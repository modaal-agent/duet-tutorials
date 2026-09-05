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
