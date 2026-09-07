// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:8a86bc6423ee24771d2e9b65a7545f274820a1bd4afe21651f275b79bee3e96a src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:8925c5b7dbaedb141340c5bfa8ae99616d96236c1239c9e92e0213b73d6eed79 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:5b2704f98a004d688e251d7230c74993b324206f55c26754851072919a360e4d src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:ce8753ca0002e26069bc554e7cdbec3d148a5e27c509eccc137443bd5c7bdfdd
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - HomeComponent
final class HomeComponent: HomeDependency {
    private let dependency: HomeDependency

    init(dependency: HomeDependency) {
        self.dependency = dependency
    }
    var items: any ItemsPort { dependency.items }
}
