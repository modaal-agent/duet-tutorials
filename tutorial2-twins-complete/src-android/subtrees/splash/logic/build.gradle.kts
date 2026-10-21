plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

// Every feature module is a Gradle project named `logic`, so each one names
// its own archive; two jars called `logic.jar` on one classpath fail the
// replay-runner's `installDist`.
base.archivesName.set("splash-logic")

// The Kotlin twin of the splash, as a plain JVM module. The iPhone app runs
// the Swift twin under src-ios/Subtrees/Splash, so nothing here compiles for
// an Apple target. The Android app and the replay-runner depend on this
// module, and its tests are the Kotlin lane.
kotlin {
  jvmToolchain(25)
}

dependencies {
  api(libs.duet.kernel)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.duet.kernel.test)
  testImplementation(kotlin("test"))
  testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  // The golden replays read parity/fixtures/*.json at runtime. Declaring the
  // directory as a task input makes a fixtures-only change re-run the tests
  // instead of leaving the task UP-TO-DATE.
  inputs.dir(rootProject.layout.projectDirectory.dir("../parity/fixtures"))
    .withPropertyName("parityFixtures")
    .withPathSensitivity(PathSensitivity.RELATIVE)
}
