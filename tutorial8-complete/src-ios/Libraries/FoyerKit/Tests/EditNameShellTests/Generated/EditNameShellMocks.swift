// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:baf50849ed61f857e1a1f158f3b2d93bcf4ec1f30bc0ae555540d17e4e334c7d template=Mocks.swifttemplate args=import=EditNameShell;import=FoyerKit
// input: sha256:d9338120763718d951cd6a81681742b8633de2e0203ecbeb07323e7e8c581725 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameBuilder.swift
// input: sha256:d3abd3d0100dd1f273cc1741c6ff2fb4ec04c9482f65700e5521cf23d32afa6b src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameView.swift
// input: sha256:fb6acdc5473c419fe593962c27557f39f6d80aa78456f5b892a83253dfd4a81a src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameViewShell.swift
// input: sha256:c4d986ab9e223c79e87eb1aef046125d303fc5ae48ee0df7cc409339e2b5f39c src-ios/Libraries/FoyerKit/Sources/EditNameShell/Generated/EditNameShellComponents.swift
// body: sha256:0ed62f0ed48f6f1c5b2b12d62d2e223a41796c00efa06df2ebb4cc90bf7eccb0
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import EditNameShell
import FoyerKit

// MARK: - EditNameDependency
final class EditNameDependencyMock: EditNameDependency {

    // MARK: - Variables
    var account: any AccountPort

    // MARK: - Initializer
    init(account: any AccountPort) {
        self.account = account
    }
}
