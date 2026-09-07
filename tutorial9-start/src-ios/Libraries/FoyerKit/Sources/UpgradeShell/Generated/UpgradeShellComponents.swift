// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:44103dc34d912f94748929c35368904fddc0604d0157c57a5522aa6cc3e421cc src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeBuilder.swift
// input: sha256:59964a1b26000406fd5361f08af1bd2a323165e62df8a7b1bc3b60712ed500c1 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeView.swift
// input: sha256:9661ba239936190f1b89a602ff41995af16782be043ec75ce02516bfb4652f8a src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeViewShell.swift
// body: sha256:c4e6c58f723ff15d3be05b22ad7cc9f136491faae0004f7f43bec6946c2dbd0a
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - UpgradeComponent
final class UpgradeComponent: UpgradeDependency {
    private let dependency: UpgradeDependency

    init(dependency: UpgradeDependency) {
        self.dependency = dependency
    }
    var purchases: any PurchasesPort { dependency.purchases }
}
