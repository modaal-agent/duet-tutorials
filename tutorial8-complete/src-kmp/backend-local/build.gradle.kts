plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("backend-local")

// The sample's backend, on device: one implementation of each port,
// persisted through the `KeyValueFile` port as one JSON document. The four
// classes are product code, not test doubles: the apps run on them, the
// composition roots construct them, and nothing replaces them later. The
// file port has one implementation per platform, in this module's jvmMain
// and appleMain source sets.
kotlin {
  jvmToolchain(25)

  jvm()
  macosArm64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(project(":ports"))
      api(libs.duet.kernel)
      api(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
