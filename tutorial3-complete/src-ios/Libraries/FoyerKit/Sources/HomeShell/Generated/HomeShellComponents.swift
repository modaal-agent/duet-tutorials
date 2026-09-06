// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:99dc63378f1d6040018856efebbeb718ed0c2960618d2938de94fcb29c07f71a src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:cfc6bfbca07d6ef775427b9411ff018d50569bae3cbca47fa81a06da0affa610 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:3dd38ea04a69d3fa2ed3acd490e03366dc7d1273edb95482eb31d1aa2cd616ef src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
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
