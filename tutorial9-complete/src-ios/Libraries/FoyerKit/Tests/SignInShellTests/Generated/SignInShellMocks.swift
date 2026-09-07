// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:401a1619801789a79367ccabc642d81dedc74329a15a0a7c0c3cdd5ad66c4766 template=Mocks.swifttemplate args=import=FoyerKit;import=SignInShell
// input: sha256:d546d23317d0a144b13eed5a1567241379e74853f23a96113db553aa441c147a src-ios/Libraries/FoyerKit/Sources/SignInShell/Generated/SignInShellComponents.swift
// input: sha256:9628adef678222dc2f49b433b22c393dde11d62229d5da1a8fc55c6378e97b95 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInBuilder.swift
// input: sha256:7c27c414c49b9a84019f0738b4aae012356a6200c68e7fcc4b021b2a8964f5f7 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInView.swift
// input: sha256:b2f7a0e3c5a9aec5b5001b729220644ffd356c6ad005de15a7a5b50ba67c6165 src-ios/Libraries/FoyerKit/Sources/SignInShell/SignInViewShell.swift
// body: sha256:8498c73e53d0dd5d27e6b7a0ace23db93a3ba20ce2874ff87df5f66ef660036a
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import SignInShell

// MARK: - SignInDependency
final class SignInDependencyMock: SignInDependency {

    // MARK: - Variables
    var analytics: any AnalyticsTracking
    var auth: any AuthPort

    // MARK: - Initializer
    init(analytics: any AnalyticsTracking, auth: any AuthPort) {
        self.analytics = analytics
        self.auth = auth
    }
}
