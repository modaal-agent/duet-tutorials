plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

// The app's telemetry grammar module: the verbs this app declares, over the
// family's telemetry artifact. The artifact carries what every app shares —
// `TrackedEvent`, `TrackedVerb`, `TrackedParam`, the `"Subject Verb"`
// encoding rule, the sink port and the fan-out worker. What stays here is
// what only this app can say: its own verbs (AppVerbs.kt). Each feature
// declares its own named events next to its reducer, from this grammar.
// `api(...)` so a feature module that depends on this one names the
// artifact's types through it; the module has no other dependency, which is
// what lets a logic module depend on it.
kotlin {
  jvmToolchain(25)

  jvm()
  macosArm64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(libs.duet.services.telemetry)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
      // The recordings are JSON; the taxonomy test reads the envelopes back.
      implementation(libs.kotlinx.serialization.json)
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
