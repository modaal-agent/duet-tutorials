// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:d6ffaf4baa5ad557aee87fee64cfbc0a4d4b0721c547839a1b072d0d6e22a867 template=Mocks.swifttemplate args=import=FoyerKit;import=ProfileShell
// input: sha256:79af9c1a5570acf5821eb579be0fad82131418e354546e66aafa4d3358ad3228 src-ios/Libraries/FoyerKit/Sources/ProfileShell/Generated/ProfileShellComponents.swift
// input: sha256:473b2ac9d817a8c1a03ef7b52c66ffed13c28e00e53a3935baf7fbe867f78b78 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:484b70cf12466f15244ce78e9890802e6646c9eccf6fd9463f5823a32623188c src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:7ad96ad23a93da19f91c0cc45cf1c00dbe5ff7e601b1377e408fdba0d65676fb src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
// body: sha256:ea922dade1a62f8ce53fe50ac21f772f119db954fc680a77a137e1f0ab576b8b
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import ProfileShell

// MARK: - ProfileDependency
final class ProfileDependencyMock: ProfileDependency {

    // MARK: - Variables
    var account: any AccountPort
    var analytics: any AnalyticsTracking
    var auth: any AuthPort

    // MARK: - Initializer
    init(account: any AccountPort, analytics: any AnalyticsTracking, auth: any AuthPort) {
        self.account = account
        self.analytics = analytics
        self.auth = auth
    }
}
