// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:d6ffaf4baa5ad557aee87fee64cfbc0a4d4b0721c547839a1b072d0d6e22a867 template=Mocks.swifttemplate args=import=FoyerKit;import=ProfileShell
// input: sha256:b13eb7be6869958d85d65f6600874cb5ebf921bcc5faf4902e1e067ffc5657fa src-ios/Libraries/FoyerKit/Sources/ProfileShell/Generated/ProfileShellComponents.swift
// input: sha256:555cc060c0d9a7b0278a1bc0d6e5433bd652d932949d2b4a83bdffd55250e63d src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:690e37d78f54007f709b7605c5e0446212fe27fe7d4dcd50adec20e1e931cbcb src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:8d423cd022c08a03ff4bb0612c561347f7fd4a3abb6b32c438fc07403a5e1fa3 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
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
