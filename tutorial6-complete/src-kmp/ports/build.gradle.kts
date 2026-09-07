plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("ports")

// The four ports the app's logic reaches the outside world through, as
// interfaces and the value types they carry. Common code only: the
// implementations live in src-kmp/backend-local. The Apple targets exist so
// the umbrella framework can export the interfaces to Swift.
kotlin {
  jvmToolchain(25)

  jvm()
  macosArm64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      // The kernel carries kotlinx.coroutines, which the two streams' StateFlows come from.
      api(libs.duet.kernel)
      implementation(libs.kotlinx.serialization.json)
    }
  }
}
