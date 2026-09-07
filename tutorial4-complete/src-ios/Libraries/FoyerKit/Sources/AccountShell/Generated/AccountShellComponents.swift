// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:c80147430854a038f582007cf20855ea287bc974b786517640b9ed745770cc8e src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountBuilder.swift
// input: sha256:923b8df5c9e6b2e3928d251f8dcc0ef7a3075ccdd337d63b47343eaed1219f3e src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountView.swift
// input: sha256:d395a6c814c8ad96f213f350660901c4614caae0e78e7d9850f2817b9f7695f3 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountViewShell.swift
// body: sha256:21e4157d31b8d7076ebe79809d37f978742aea52b03a9790370cc4cf17ce2b19
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
    var auth: any AuthPort { dependency.auth }
}
