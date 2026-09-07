// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:d9338120763718d951cd6a81681742b8633de2e0203ecbeb07323e7e8c581725 src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameBuilder.swift
// input: sha256:d3abd3d0100dd1f273cc1741c6ff2fb4ec04c9482f65700e5521cf23d32afa6b src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameView.swift
// input: sha256:fb6acdc5473c419fe593962c27557f39f6d80aa78456f5b892a83253dfd4a81a src-ios/Libraries/FoyerKit/Sources/EditNameShell/EditNameViewShell.swift
// body: sha256:7e053a919fe37bf24f877031dae2eaae1d396c496a9dde0c5f43b585f27e136c
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - EditNameComponent
final class EditNameComponent: EditNameDependency {
    private let dependency: EditNameDependency

    init(dependency: EditNameDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
}
