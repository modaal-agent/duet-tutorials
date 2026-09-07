// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:59ad4251e35c863e18c6b61a9afa3c26654700b2e583e92d27199caaf342b3b5 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:bd52ab8a881463b805aec36ea8d278b3a84772879e70c7db933e5a473e6d4681 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:d4e3812b51e88791ee8e37124f8bc1dec6dc1caa70cd904601f10d6e9f0bea97 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:32664df0d90e195b72d024105173b5bde61bd45ca4af0a5644e1d47ce470f2b5
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
    var purchases: any PurchasesPort { dependency.purchases }
}
