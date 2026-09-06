import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// The Apple boundary: one Kotlin/Native framework aggregating the kernel and
// every feature module. One framework rather than one per feature, because a
// static Kotlin/Native framework embeds the Kotlin runtime and two of them
// would carry it twice. Nothing publishes from here; scripts/assemble_kit.sh
// builds the XCFramework and copies it to the one path both Swift consumers
// (swift-consumer/ and src-ios/Libraries/FoyerKit) link.
//
// arm64 slices only: device, simulator and macOS. A generic simulator build
// without `ARCHS=arm64` also compiles x86_64, where this framework has no
// slice and every exported declaration is out of scope.
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.skie)
}

kotlin {
  jvmToolchain(25)

  val xcf = XCFramework("FoyerKit")

  listOf(
    macosArm64(),
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { target ->
    target.binaries.framework {
      baseName = "FoyerKit"
      isStatic = true
      binaryOption("bundleId", "dev.modaal.foyer.kit")
      // `export` puts a dependency's declarations in the framework's headers.
      // A module that is linked but not exported compiles, and Swift cannot
      // name a single type from it. One line per feature module, plus the
      // ports, whose interfaces the Swift mock services implement.
      export(dependencies.project(":ports"))
      export(dependencies.project(":subtrees:splash:logic"))
      export(dependencies.project(":subtrees:root:logic"))
      export(dependencies.project(":subtrees:signin:logic"))
      export(dependencies.project(":subtrees:main:logic"))
      export(dependencies.project(":subtrees:home:logic"))
      export(dependencies.project(":subtrees:profile:logic"))
      export(dependencies.project(":subtrees:account:logic"))
      export(dependencies.project(":subtrees:editname:logic"))
      export(libs.duet.kernel)
      xcf.add(this)
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(project(":ports"))
      api(project(":subtrees:splash:logic"))
      api(project(":subtrees:root:logic"))
      api(project(":subtrees:signin:logic"))
      api(project(":subtrees:main:logic"))
      api(project(":subtrees:home:logic"))
      api(project(":subtrees:profile:logic"))
      api(project(":subtrees:account:logic"))
      api(project(":subtrees:editname:logic"))
      api(libs.duet.kernel)
    }
  }
}

// No SKIE analytics collection or upload: the framework build stays hermetic.
skie {
  analytics {
    enabled.set(false)
    disableUpload.set(true)
  }
}
