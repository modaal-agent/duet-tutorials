// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:8cd57e157298998e223939d3a83f9f2b1b1dd69d595cd4dd8f7052cac56f0dfa template=Mocks.swifttemplate args=import=FoyerKit;import=HomeShell
// input: sha256:fa4e00b186c9a466ae78ddde18417b6b812682b1c090b2bec1e94b8dee7ab5bf src-ios/Libraries/FoyerKit/Sources/HomeShell/Generated/HomeShellComponents.swift
// input: sha256:99dc63378f1d6040018856efebbeb718ed0c2960618d2938de94fcb29c07f71a src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:cfc6bfbca07d6ef775427b9411ff018d50569bae3cbca47fa81a06da0affa610 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:3dd38ea04a69d3fa2ed3acd490e03366dc7d1273edb95482eb31d1aa2cd616ef src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
// body: sha256:8f4145f87e0d283d6f0b58a4f897cbf760491cfa5ccf50e29a8897c54da1735b
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import HomeShell

// MARK: - HomeDependency
final class HomeDependencyMock: HomeDependency {

    // MARK: - Variables
    var items: any ItemsPort

    // MARK: - Initializer
    init(items: any ItemsPort) {
        self.items = items
    }
}
