plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  // The Apple framework's Swift projection, applied by :apple-umbrella.
  alias(libs.plugins.skie) apply false
  // The Android app: AGP 9, whose built-in Kotlin compiles it, and the Compose
  // compiler plugin. Both resolve here once so the app module applies them
  // without restating a version.
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
}

// `tools/duet record` passes -PregenFixtures=1; the test JVMs read the system
// property (a Gradle property does not cross the daemon boundary by itself).
subprojects {
  val regenFixtures = providers.gradleProperty("regenFixtures")
  // Every tutorial's closing exercise is a deliberately failing test class
  // named Tutorial<N>Exercise…; the repository's CI sets TUTORIAL_SKIP_STUBS=1
  // to leave it out. A bare `tools/duet verify` includes it.
  val skipStubs = providers.environmentVariable("TUTORIAL_SKIP_STUBS").orNull == "1"
  tasks.withType<Test>().configureEach {
    systemProperty("duet.regenFixtures", regenFixtures.getOrElse(""))
    if (skipStubs) exclude("**/Tutorial*Exercise*")
  }
}
