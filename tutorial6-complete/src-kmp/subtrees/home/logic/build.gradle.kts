plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

// Every feature module is a Gradle project named `logic`, so each one names
// its own archive; two jars called `logic-jvm.jar` on one classpath fail the
// replay-runner's `installDist`.
base.archivesName.set("home-logic")

// The feature's one implementation lives in commonMain. The JVM target runs
// the host test lane; the Apple targets compile the same sources for the iOS
// app through the umbrella framework.
kotlin {
  jvmToolchain(25)

  jvm()
  macosArm64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(libs.duet.kernel)
      implementation(libs.kotlinx.serialization.json)
      // The items port and the Item type the state carries.
      api(project(":ports"))
    }
    jvmTest.dependencies {
      implementation(libs.duet.kernel.test)
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
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

// The mock engine: a `<Name>Mock` recording spy for the environment interface,
// generated into build/generated/ksp at test compilation. Nothing is
// committed, and a member added to the interface fails the next test compile
// against the regenerated mock.
dependencies {
  "kspJvmTest"(libs.mocks.processor)
}

ksp {
  arg("kspMocksTargets", "dev.modaal.foyer.home.HomeEnvironment")
}
