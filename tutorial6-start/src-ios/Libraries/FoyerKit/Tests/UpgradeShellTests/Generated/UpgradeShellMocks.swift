// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:52785f6745d124cc1f0d66c6229677c9a10a6151d5708eb3c39e4ab91270189a template=Mocks.swifttemplate args=import=FoyerKit;import=UpgradeShell
// input: sha256:4c4e46e1a59051ff14b3cdaf9c0197133919fa26f4b9ef050417a6fe458d261e src-ios/Libraries/FoyerKit/Sources/UpgradeShell/Generated/UpgradeShellComponents.swift
// input: sha256:44103dc34d912f94748929c35368904fddc0604d0157c57a5522aa6cc3e421cc src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeBuilder.swift
// input: sha256:59964a1b26000406fd5361f08af1bd2a323165e62df8a7b1bc3b60712ed500c1 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeView.swift
// input: sha256:9661ba239936190f1b89a602ff41995af16782be043ec75ce02516bfb4652f8a src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeViewShell.swift
// body: sha256:685d5b42bd031e0c23633e40ed34594d56e979b8af213611653ee85feb90c81e
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import UpgradeShell

// MARK: - UpgradeDependency
final class UpgradeDependencyMock: UpgradeDependency {

    // MARK: - Variables
    var purchases: any PurchasesPort

    // MARK: - Initializer
    init(purchases: any PurchasesPort) {
        self.purchases = purchases
    }
}
