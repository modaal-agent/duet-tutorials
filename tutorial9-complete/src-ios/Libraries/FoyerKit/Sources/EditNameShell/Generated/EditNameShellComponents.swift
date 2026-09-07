// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:077c2f4a501eb00d93a327b5f6fcaebb8025916c543c3a6c85ceac5b4b2ae1ff src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameBuilder.swift
// input: sha256:3e2fab4b31d0ea83116b1e314b1d12587682739d869cbde524977a7507d27a8c src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameView.swift
// input: sha256:32fcc568b3c94d15d49c4a0f44975ad24acbf517fc1bedd6e0f6c64d70ea1d37 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameViewShell.swift
// body: sha256:fa38a63bd01a46627e466cbfd379e32cb6b9d31eaf91b08bacc6a93e1dea4c45
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - EditNameComponent
final class EditNameComponent: EditNameDependency {
    private let dependency: EditNameDependency

    init(dependency: EditNameDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
    var analytics: any AnalyticsTracking { dependency.analytics }
}
