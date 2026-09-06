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
  implementation(project(":subtrees:splash:logic"))
}

application {
  mainClass.set("dev.modaal.foyer.replayrunner.MainKt")
}
