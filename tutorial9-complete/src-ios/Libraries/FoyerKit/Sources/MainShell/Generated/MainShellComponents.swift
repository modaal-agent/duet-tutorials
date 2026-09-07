// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:b963bfdb799b354d19bd95a339cfc812d2088882bb456a421fde197607a13ad2 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:21531b7f2cd0baef52b46b59ee2edc4c9697c9be26bd7870554a449bba3d3149 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:2af853b5b6524454a314671b90c6754c9e4b8036aaf57e4bdee8cecb74898e80 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:34ebcd3dfc9eecb306d2e5206642e7c35fadceb6824af7507c103ecbe60d890a
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - MainComponent
final class MainComponent: MainDependency {
    private let dependency: MainDependency

    init(dependency: MainDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
    var analytics: any AnalyticsTracking { dependency.analytics }
    var auth: any AuthPort { dependency.auth }
    var items: any ItemsPort { dependency.items }
    var purchases: any PurchasesPort { dependency.purchases }
}
