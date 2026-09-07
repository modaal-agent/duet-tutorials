// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:d6ffaf4baa5ad557aee87fee64cfbc0a4d4b0721c547839a1b072d0d6e22a867 template=Mocks.swifttemplate args=import=FoyerKit;import=ProfileShell
// input: sha256:1c01c3c7b52335d169957e5b3810e4fd90edfb16a4e0821a1bb0cd3f9d0f8d47 src-ios/Libraries/FoyerKit/Sources/ProfileShell/Generated/ProfileShellComponents.swift
// input: sha256:436dabea5870bfd0333d417f43dfac99c5d3986d1702c0d22407244ef1ecb350 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:30219e3661fab6c31bbb14d6a83ba0784f47e68adbd11774feb310cc3feb6118 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:4122ea58d240c332f8c2a242738ec0ad0eba5b606bd1d4d66fc9642aaae479a8 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
// body: sha256:33874041dc9d01ac097e448ff009ee2063c1a58c63d21902eb784686fb8630b9
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import ProfileShell

// MARK: - ProfileDependency
final class ProfileDependencyMock: ProfileDependency {

    // MARK: - Variables
    var account: any AccountPort
    var auth: any AuthPort

    // MARK: - Initializer
    init(account: any AccountPort, auth: any AuthPort) {
        self.account = account
        self.auth = auth
    }
}
