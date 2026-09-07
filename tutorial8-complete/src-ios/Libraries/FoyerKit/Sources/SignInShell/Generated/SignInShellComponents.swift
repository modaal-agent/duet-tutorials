// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:967227275b8eb180fde0a120a0f7b5e3e85cc59644d3122ba574012c4378f49a src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInBuilder.swift
// input: sha256:51f48f6375d3122afb64c39ae3156d21e75e166dd6372c2ebfb4c6fc0082590c src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInView.swift
// input: sha256:402224eea4039923bc880a3b8ede993485d24fd6b292f8104408bcb60d422802 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInViewShell.swift
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
