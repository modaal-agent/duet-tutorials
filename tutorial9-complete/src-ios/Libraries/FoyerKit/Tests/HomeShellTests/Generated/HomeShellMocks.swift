// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:8cd57e157298998e223939d3a83f9f2b1b1dd69d595cd4dd8f7052cac56f0dfa template=Mocks.swifttemplate args=import=FoyerKit;import=HomeShell
// input: sha256:616c31f27b1bbe4ce372edbe0f5ff0c83d16ac1a30f3eda0983140b1a54391be src-ios/Libraries/FoyerKit/Sources/HomeShell/Generated/HomeShellComponents.swift
// input: sha256:6635133f3de4fa0de883a429cad4bf14998d545f4828a4023cffbba52d50419f src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:8925c5b7dbaedb141340c5bfa8ae99616d96236c1239c9e92e0213b73d6eed79 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:5b2704f98a004d688e251d7230c74993b324206f55c26754851072919a360e4d src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:8fe1e88f877ec0e537613ece2f76c9750a352a3bba3ce4ccd3195559c01162a9
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import HomeShell

// MARK: - HomeDependency
final class HomeDependencyMock: HomeDependency {

    // MARK: - Variables
    var analytics: any AnalyticsTracking
    var items: any ItemsPort

    // MARK: - Initializer
    init(analytics: any AnalyticsTracking, items: any ItemsPort) {
        self.analytics = analytics
        self.items = items
    }
}
