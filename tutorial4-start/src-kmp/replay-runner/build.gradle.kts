plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

kotlin {
  jvmToolchain(25)
}

// The replay-protocol endpoint: a stdio JSON-lines server over the app's
// feature registry. `tools/duet protocol-run` drives it after
// `./gradlew :replay-runner:installDist`. Nothing in the apps depends on it.
dependencies {
  implementation(libs.duet.kernel)
  // One line per feature module, matching the registry in Main.kt.
  implementation(project(":subtrees:splash:logic"))
  implementation(project(":subtrees:root:logic"))
  implementation(project(":subtrees:signin:logic"))
  implementation(project(":subtrees:main:logic"))
  implementation(project(":subtrees:home:logic"))
  implementation(project(":subtrees:profile:logic"))
  implementation(project(":subtrees:account:logic"))
  implementation(project(":subtrees:editname:logic"))
}

application {
  mainClass.set("dev.modaal.foyer.replayrunner.MainKt")
}
