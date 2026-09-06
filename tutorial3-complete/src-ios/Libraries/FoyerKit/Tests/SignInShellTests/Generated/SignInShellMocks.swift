// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:401a1619801789a79367ccabc642d81dedc74329a15a0a7c0c3cdd5ad66c4766 template=Mocks.swifttemplate args=import=FoyerKit;import=SignInShell
// input: sha256:4fb5b66820fea3d35bc05a12e1e950440858cd6d1cb957b46cdc7b39c0fb8bef src-ios/Libraries/FoyerKit/Sources/SignInShell/Generated/SignInShellComponents.swift
// input: sha256:967227275b8eb180fde0a120a0f7b5e3e85cc59644d3122ba574012c4378f49a src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInBuilder.swift
// input: sha256:7c27c414c49b9a84019f0738b4aae012356a6200c68e7fcc4b021b2a8964f5f7 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInView.swift
// input: sha256:b2f7a0e3c5a9aec5b5001b729220644ffd356c6ad005de15a7a5b50ba67c6165 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInViewShell.swift
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
