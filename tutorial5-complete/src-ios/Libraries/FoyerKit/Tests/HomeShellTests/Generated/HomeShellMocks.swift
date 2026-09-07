// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:8cd57e157298998e223939d3a83f9f2b1b1dd69d595cd4dd8f7052cac56f0dfa template=Mocks.swifttemplate args=import=FoyerKit;import=HomeShell
// input: sha256:7aab429059ae66c45b53e320e011b8b44425924ee4271a9ed53c6560c5bcfa71 src-ios/Libraries/FoyerKit/Sources/HomeShell/Generated/HomeShellComponents.swift
// input: sha256:8a86bc6423ee24771d2e9b65a7545f274820a1bd4afe21651f275b79bee3e96a src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
// input: sha256:8925c5b7dbaedb141340c5bfa8ae99616d96236c1239c9e92e0213b73d6eed79 src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeView.swift
// input: sha256:5b2704f98a004d688e251d7230c74993b324206f55c26754851072919a360e4d src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeViewShell.swift
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
