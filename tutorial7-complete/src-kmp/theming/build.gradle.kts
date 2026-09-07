plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

base.archivesName.set("theming")

// The theming module: the token vocabularies and the value table generated
// from parity/design-tokens.yaml (`Generated/`, written by `tools/duet
// design-tokens` and gated by `tools/duet design-tokens --check`), and the
// two themes over them, hand-authored in Theme.kt. The JVM target is the only
// one: the Android app consumes this module's JVM variant, and the iOS app
// reads the same config through its own engine, so no Apple slice has a
// consumer.
kotlin {
  jvmToolchain(25)

  jvm()

  sourceSets {
    commonMain.dependencies {
      // ColorToken and FontToken, the value types the generated table is
      // written in.
      api(libs.duet.services.theming)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
