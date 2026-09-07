// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:d6ffaf4baa5ad557aee87fee64cfbc0a4d4b0721c547839a1b072d0d6e22a867 template=Mocks.swifttemplate args=import=FoyerKit;import=ProfileShell
// input: sha256:9f648241811adc7bf8d5df91dfbd52254e60ed82e10b521f3dcbb2da44e8dcca src-ios/Libraries/FoyerKit/Sources/ProfileShell/Generated/ProfileShellComponents.swift
// input: sha256:436dabea5870bfd0333d417f43dfac99c5d3986d1702c0d22407244ef1ecb350 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:484b70cf12466f15244ce78e9890802e6646c9eccf6fd9463f5823a32623188c src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:7ad96ad23a93da19f91c0cc45cf1c00dbe5ff7e601b1377e408fdba0d65676fb src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
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
