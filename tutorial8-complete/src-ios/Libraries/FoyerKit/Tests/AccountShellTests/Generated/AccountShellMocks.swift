// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:ac2c397b162dff46ca34da2097d6bd8344fcd72fbb5184f68bae54d2cd74c8b6 template=Mocks.swifttemplate args=import=AccountShell;import=FoyerKit
// input: sha256:bfd1f47aac4fbf7e2ca5c89bf394ebed68408db5c75af047563d006eac1f73d0 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountBuilder.swift
// input: sha256:422874ccb540611ed2a2242775fd596110aeea6caf060df6d1dcffc2fa7d20b1 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountView.swift
// input: sha256:d395a6c814c8ad96f213f350660901c4614caae0e78e7d9850f2817b9f7695f3 src-ios/Libraries/FoyerKit/Sources/AccountShell/AccountViewShell.swift
// input: sha256:c51b788454dd8418234a3dc9b883602478b7eb68664314dd559630bc7f9d299b src-ios/Libraries/FoyerKit/Sources/AccountShell/Generated/AccountShellComponents.swift
// body: sha256:95c9c4f79ab04ec7a1662c28b225701090f8bd065495444e3a022f710d04eeb5
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import AccountShell
import FoyerKit

// MARK: - AccountDependency
final class AccountDependencyMock: AccountDependency {

    // MARK: - Variables
    var account: any AccountPort
    var auth: any AuthPort

    // MARK: - Initializer
    init(account: any AccountPort, auth: any AuthPort) {
        self.account = account
        self.auth = auth
    }
}
