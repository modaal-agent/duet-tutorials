// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:52785f6745d124cc1f0d66c6229677c9a10a6151d5708eb3c39e4ab91270189a template=Mocks.swifttemplate args=import=FoyerKit;import=UpgradeShell
// input: sha256:f5e944f18cce31a1af9d2f3140bb341494c07c1b0254484d5ccbb060d9ad4953 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/Generated/UpgradeShellComponents.swift
// input: sha256:44103dc34d912f94748929c35368904fddc0604d0157c57a5522aa6cc3e421cc src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeBuilder.swift
// input: sha256:6b0142abce406175cadf8726e2fa31c487eaaf85518430733acdb52cb9510f7c src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeView.swift
// input: sha256:10150ac80ad9b9691f096b5971cf918c4c778214dad393337d6f35b5349d405c src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeViewShell.swift
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
