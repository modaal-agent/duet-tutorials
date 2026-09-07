// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:52785f6745d124cc1f0d66c6229677c9a10a6151d5708eb3c39e4ab91270189a template=Mocks.swifttemplate args=import=FoyerKit;import=UpgradeShell
// input: sha256:d376625694debfd84503438c19ee481eb131164f36acc4860fd4b204c3aeefeb src-ios/Libraries/FoyerKit/Sources/UpgradeShell/Generated/UpgradeShellComponents.swift
// input: sha256:02653c185a8336b8f23bd392ac64afcf5c8a5082935cfde3c496d798b95a2332 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeBuilder.swift
// input: sha256:59964a1b26000406fd5361f08af1bd2a323165e62df8a7b1bc3b60712ed500c1 src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeView.swift
// input: sha256:9661ba239936190f1b89a602ff41995af16782be043ec75ce02516bfb4652f8a src-ios/Libraries/FoyerKit/Sources/UpgradeShell/UpgradeViewShell.swift
// body: sha256:30191478550be135b74e5615528aa53a55e1c0a8890cecbb6a5de1e23dc65983
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import UpgradeShell

// MARK: - UpgradeDependency
final class UpgradeDependencyMock: UpgradeDependency {

    // MARK: - Variables
    var analytics: any AnalyticsTracking
    var purchases: any PurchasesPort

    // MARK: - Initializer
    init(analytics: any AnalyticsTracking, purchases: any PurchasesPort) {
        self.analytics = analytics
        self.purchases = purchases
    }
}
