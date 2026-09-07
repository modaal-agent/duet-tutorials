// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:9d0b93179b055420a0e89a453cd6b013d73589accba3e80c0222fbcaf50f4ff2 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:b0c2ca2df34f7464731c91a8b407b1161d6522a7c988aab2cd64399a081f6308 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:2af853b5b6524454a314671b90c6754c9e4b8036aaf57e4bdee8cecb74898e80 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:b92554e15e95fbd628ed4edbe94289aed8f3fa2c752e1fbc4f2d9e3b63f1463f
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
    var auth: any AuthPort { dependency.auth }
    var items: any ItemsPort { dependency.items }
    var purchases: any PurchasesPort { dependency.purchases }
}
