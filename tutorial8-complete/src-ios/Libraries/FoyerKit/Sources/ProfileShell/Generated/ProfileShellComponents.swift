// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:436dabea5870bfd0333d417f43dfac99c5d3986d1702c0d22407244ef1ecb350 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:30219e3661fab6c31bbb14d6a83ba0784f47e68adbd11774feb310cc3feb6118 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:4122ea58d240c332f8c2a242738ec0ad0eba5b606bd1d4d66fc9642aaae479a8 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
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
