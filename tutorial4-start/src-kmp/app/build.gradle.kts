// The Android app. AGP 9's built-in Kotlin compiles it; the feature modules
// keep the multiplatform plugin, and this module consumes their JVM variant.
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "dev.modaal.foyer.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "dev.modaal.foyer"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "0.1"
  }

  buildTypes {
    debug {}
  }
}

kotlin {
  jvmToolchain(25)
}

dependencies {
  // The feature modules, one line each. Logic stays in src-kmp/subtrees; the
  // screens over it live in this module.
  implementation(project(":ports"))
  implementation(project(":subtrees:splash:logic"))
  implementation(project(":subtrees:root:logic"))
  implementation(project(":subtrees:signin:logic"))
  implementation(project(":subtrees:main:logic"))
  implementation(project(":subtrees:home:logic"))
  implementation(project(":subtrees:profile:logic"))
  implementation(project(":subtrees:account:logic"))
  implementation(project(":subtrees:editname:logic"))
  implementation(libs.duet.kernel)
  // StoreHost, ChildSlot, StateTransitions and RetainedRoot: the teardown
  // registry, the child reconciler, the state observation and the retained
  // carrier the Activity keeps the app on across rotation.
  implementation(libs.duet.shells.compose)
  implementation(libs.essenty.instance.keeper)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)

  // AGP's built-in Kotlin does not map kotlin("test") to a framework; pin JUnit.
  testImplementation(kotlin("test-junit"))
  testImplementation(libs.kotlinx.coroutines.test)
}
