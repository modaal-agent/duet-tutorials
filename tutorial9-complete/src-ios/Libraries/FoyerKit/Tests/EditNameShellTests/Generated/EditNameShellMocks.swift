// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:baf50849ed61f857e1a1f158f3b2d93bcf4ec1f30bc0ae555540d17e4e334c7d template=Mocks.swifttemplate args=import=EditNameShell;import=FoyerKit
// input: sha256:077c2f4a501eb00d93a327b5f6fcaebb8025916c543c3a6c85ceac5b4b2ae1ff src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameBuilder.swift
// input: sha256:3e2fab4b31d0ea83116b1e314b1d12587682739d869cbde524977a7507d27a8c src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameView.swift
// input: sha256:32fcc568b3c94d15d49c4a0f44975ad24acbf517fc1bedd6e0f6c64d70ea1d37 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameViewShell.swift
// input: sha256:e83300d01c288f2783622e8ef802572ededd9edc502e3fc9705eb97269e3fa61 src-ios/Libraries/FoyerKit/Sources/EditNameShell/Generated/EditNameShellComponents.swift
// body: sha256:40f9a06585a40b2d696974159a8a58b3284736ba08d7570d1b482c50bb819c46
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import EditNameShell
import FoyerKit

// MARK: - EditNameDependency
final class EditNameDependencyMock: EditNameDependency {

    // MARK: - Variables
    var account: any AccountPort
    var analytics: any AnalyticsTracking

    // MARK: - Initializer
    init(account: any AccountPort, analytics: any AnalyticsTracking) {
        self.account = account
        self.analytics = analytics
    }
}
