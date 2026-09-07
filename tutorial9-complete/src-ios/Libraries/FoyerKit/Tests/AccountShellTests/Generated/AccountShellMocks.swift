// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:ac2c397b162dff46ca34da2097d6bd8344fcd72fbb5184f68bae54d2cd74c8b6 template=Mocks.swifttemplate args=import=AccountShell;import=FoyerKit
// input: sha256:f222d889d662838f1c982728c7c25149dfb3ed5359048e600fffc9bbf6269324 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountBuilder.swift
// input: sha256:923b8df5c9e6b2e3928d251f8dcc0ef7a3075ccdd337d63b47343eaed1219f3e src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountView.swift
// input: sha256:d395a6c814c8ad96f213f350660901c4614caae0e78e7d9850f2817b9f7695f3 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountViewShell.swift
// input: sha256:2ba62da3af7601ebc592e77d011599528b853569b8074aa832d88846eeff097e src-ios/Libraries/FoyerKit/Sources/AccountShell/Generated/AccountShellComponents.swift
// body: sha256:26d9869696922ccd9706fde3a58bc26ea9b1d098d92d18a581beee0428882169
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import AccountShell
import FoyerKit

// MARK: - AccountDependency
final class AccountDependencyMock: AccountDependency {

    // MARK: - Variables
    var account: any AccountPort
    var analytics: any AnalyticsTracking
    var auth: any AuthPort

    // MARK: - Initializer
    init(account: any AccountPort, analytics: any AnalyticsTracking, auth: any AuthPort) {
        self.account = account
        self.analytics = analytics
        self.auth = auth
    }
}
