// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:baf50849ed61f857e1a1f158f3b2d93bcf4ec1f30bc0ae555540d17e4e334c7d template=Mocks.swifttemplate args=import=EditNameShell;import=FoyerKit
// input: sha256:d9338120763718d951cd6a81681742b8633de2e0203ecbeb07323e7e8c581725 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameBuilder.swift
// input: sha256:3e2fab4b31d0ea83116b1e314b1d12587682739d869cbde524977a7507d27a8c src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameView.swift
// input: sha256:32fcc568b3c94d15d49c4a0f44975ad24acbf517fc1bedd6e0f6c64d70ea1d37 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameViewShell.swift
// input: sha256:47c46f816e6f2ff7adca94ea81d6c0de39e7b1cc54e596942f47a9bb32825b8a src-ios/Libraries/FoyerKit/Sources/EditNameShell/Generated/EditNameShellComponents.swift
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
