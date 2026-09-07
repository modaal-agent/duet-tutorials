// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:555cc060c0d9a7b0278a1bc0d6e5433bd652d932949d2b4a83bdffd55250e63d src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:ddbd680d4f27a791b2e9a853524c56ce75303053b78e46f3d121482a4d7e9152 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:d9d04301d60e8decfb198851eca1339c0266e186fdc379cdbdaf65154da0f3c5 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
// body: sha256:71bd145bdf298135438b0587a1cc9df83c3d4c1054a5a850e0bded60a4b671c9
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - ProfileComponent
final class ProfileComponent: ProfileDependency {
    private let dependency: ProfileDependency

    init(dependency: ProfileDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
    var auth: any AuthPort { dependency.auth }
}
