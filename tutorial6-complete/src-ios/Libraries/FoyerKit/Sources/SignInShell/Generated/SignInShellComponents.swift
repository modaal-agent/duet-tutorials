// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:967227275b8eb180fde0a120a0f7b5e3e85cc59644d3122ba574012c4378f49a src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInBuilder.swift
// input: sha256:7c27c414c49b9a84019f0738b4aae012356a6200c68e7fcc4b021b2a8964f5f7 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInView.swift
// input: sha256:b2f7a0e3c5a9aec5b5001b729220644ffd356c6ad005de15a7a5b50ba67c6165 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInViewShell.swift
// body: sha256:771e97677ba0fba45afd721024d953e8663ab7a74840f04232ddedf420358659
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - SignInComponent
final class SignInComponent: SignInDependency {
    private let dependency: SignInDependency

    init(dependency: SignInDependency) {
        self.dependency = dependency
    }
    var auth: any AuthPort { dependency.auth }
}
