// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:401a1619801789a79367ccabc642d81dedc74329a15a0a7c0c3cdd5ad66c4766 template=Mocks.swifttemplate args=import=FoyerKit;import=SignInShell
// input: sha256:c378516910eccd090d438f771a328a6cba11f71d7fdc414820552d4136514b88 src-ios/Libraries/FoyerKit/Sources/SignInShell/Generated/SignInShellComponents.swift
// input: sha256:967227275b8eb180fde0a120a0f7b5e3e85cc59644d3122ba574012c4378f49a src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInBuilder.swift
// input: sha256:51f48f6375d3122afb64c39ae3156d21e75e166dd6372c2ebfb4c6fc0082590c src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInView.swift
// input: sha256:402224eea4039923bc880a3b8ede993485d24fd6b292f8104408bcb60d422802 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInViewShell.swift
// body: sha256:8a2cf4e5546e37bb2b6b69603fd0f571a00b364d9d71d5a5496f2ad45662778a
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import SignInShell

// MARK: - SignInDependency
final class SignInDependencyMock: SignInDependency {

    // MARK: - Variables
    var auth: any AuthPort

    // MARK: - Initializer
    init(auth: any AuthPort) {
        self.auth = auth
    }
}
