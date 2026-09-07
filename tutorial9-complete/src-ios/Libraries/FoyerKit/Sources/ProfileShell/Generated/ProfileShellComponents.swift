// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:473b2ac9d817a8c1a03ef7b52c66ffed13c28e00e53a3935baf7fbe867f78b78 src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileBuilder.swift
// input: sha256:484b70cf12466f15244ce78e9890802e6646c9eccf6fd9463f5823a32623188c src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileView.swift
// input: sha256:7ad96ad23a93da19f91c0cc45cf1c00dbe5ff7e601b1377e408fdba0d65676fb src-ios/Libraries/FoyerKit/Sources/ProfileShell/ProfileViewShell.swift
// body: sha256:853de6118b18b09edd54081470508e84691fe98a95baa6e72ce973ed1f4935ed
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
    var analytics: any AnalyticsTracking { dependency.analytics }
    var auth: any AuthPort { dependency.auth }
}
