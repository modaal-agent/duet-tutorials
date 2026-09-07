// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:8cd57e157298998e223939d3a83f9f2b1b1dd69d595cd4dd8f7052cac56f0dfa template=Mocks.swifttemplate args=import=FoyerKit;import=HomeShell
// input: sha256:f4484f46c1079ff9ddf4b5e53a86dfc4f27c0dd55c043c4c7c36f3a1fa723994 src-ios/Libraries/FoyerKit/Sources/HomeShell/Generated/HomeShellComponents.swift
// input: sha256:59ad4251e35c863e18c6b61a9afa3c26654700b2e583e92d27199caaf342b3b5 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:bd52ab8a881463b805aec36ea8d278b3a84772879e70c7db933e5a473e6d4681 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:d4e3812b51e88791ee8e37124f8bc1dec6dc1caa70cd904601f10d6e9f0bea97 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:c1c0ca267fb15902c971f61df25bef243e4928a1064962ed00c36ddcb2e5b4c1
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import HomeShell

// MARK: - HomeDependency
final class HomeDependencyMock: HomeDependency {

    // MARK: - Variables
    var items: any ItemsPort
    var purchases: any PurchasesPort

    // MARK: - Initializer
    init(items: any ItemsPort, purchases: any PurchasesPort) {
        self.items = items
        self.purchases = purchases
    }
}
