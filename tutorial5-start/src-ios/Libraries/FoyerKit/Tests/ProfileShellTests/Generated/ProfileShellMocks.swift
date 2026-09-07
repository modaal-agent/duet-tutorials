// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:d6ffaf4baa5ad557aee87fee64cfbc0a4d4b0721c547839a1b072d0d6e22a867 template=Mocks.swifttemplate args=import=FoyerKit;import=ProfileShell
// input: sha256:ff439514fe954f9e224f7c72533e5b0baf001f857b686bd78a6eadd81726c207 src-ios/Libraries/FoyerKit/Sources/ProfileShell/Generated/ProfileShellComponents.swift
// input: sha256:555cc060c0d9a7b0278a1bc0d6e5433bd652d932949d2b4a83bdffd55250e63d src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:ddbd680d4f27a791b2e9a853524c56ce75303053b78e46f3d121482a4d7e9152 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:d9d04301d60e8decfb198851eca1339c0266e186fdc379cdbdaf65154da0f3c5 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
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
