// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:f222d889d662838f1c982728c7c25149dfb3ed5359048e600fffc9bbf6269324 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountBuilder.swift
// input: sha256:923b8df5c9e6b2e3928d251f8dcc0ef7a3075ccdd337d63b47343eaed1219f3e src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountView.swift
// input: sha256:d395a6c814c8ad96f213f350660901c4614caae0e78e7d9850f2817b9f7695f3 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountViewShell.swift
// body: sha256:e2537c6ad1820c69b4c7c2948f06e8603a86b1d974518d8dce69e384474b7a81
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - AccountComponent
final class AccountComponent: AccountDependency {
    private let dependency: AccountDependency

    init(dependency: AccountDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
    var analytics: any AnalyticsTracking { dependency.analytics }
    var auth: any AuthPort { dependency.auth }
}
