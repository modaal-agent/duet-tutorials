plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
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
