plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

base.archivesName.set("ports")

// The four ports the app's logic reaches the outside world through, as
// interfaces and the value types they carry. Common code only: the
// implementations live in each app (the mock services in Tutorial 3, the
// on-device backend from Tutorial 4). The Apple targets exist so the umbrella
// framework can export the interfaces to Swift.
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
    }
  }
}
