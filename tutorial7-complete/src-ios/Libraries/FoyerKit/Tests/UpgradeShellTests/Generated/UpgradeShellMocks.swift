// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:52785f6745d124cc1f0d66c6229677c9a10a6151d5708eb3c39e4ab91270189a template=Mocks.swifttemplate args=import=FoyerKit;import=UpgradeShell
// input: sha256:522c98259a4e88dd29143ecca2dc8a0b75df7d3fbfeb731dd0998089cc3c8acb src-ios/Libraries/FoyerKit/Sources/UpgradeShell/Generated/UpgradeShellComponents.swift
// input: sha256:44103dc34d912f94748929c35368904fddc0604d0157c57a5522aa6cc3e421cc src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeBuilder.swift
// input: sha256:8b1d90d4b088449b6b5f7b6fc59e69f03972b5fa54c3c93a8642de64d2c9a447 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeView.swift
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
