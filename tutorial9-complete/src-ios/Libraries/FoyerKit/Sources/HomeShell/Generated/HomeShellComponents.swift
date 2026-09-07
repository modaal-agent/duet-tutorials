// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:6635133f3de4fa0de883a429cad4bf14998d545f4828a4023cffbba52d50419f src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:8925c5b7dbaedb141340c5bfa8ae99616d96236c1239c9e92e0213b73d6eed79 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:5b2704f98a004d688e251d7230c74993b324206f55c26754851072919a360e4d src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:6131894637aa278677c0414ce2d9cfea76a65393d0cb914aa6479b53e0eb4709
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
    var analytics: any AnalyticsTracking { dependency.analytics }
    var items: any ItemsPort { dependency.items }
}
