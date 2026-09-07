// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:3d0cf92f895fb2af9221bc12ea3ae696d0acefe83a5bcb0bacb7fd0fa4d16612 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:ba6c3c3dad2a82df2314539783e206d5bef1a71efbca259faadcf946fb1525e4 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:85a8ef5ca4957dad3bf9d6d27e6bc251bb8ce104c4c1439d04b8f92aaea2aac1 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:b92554e15e95fbd628ed4edbe94289aed8f3fa2c752e1fbc4f2d9e3b63f1463f
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit

// MARK: - MainComponent
final class MainComponent: MainDependency {
    private let dependency: MainDependency

    init(dependency: MainDependency) {
        self.dependency = dependency
    }
    var account: any AccountPort { dependency.account }
    var auth: any AuthPort { dependency.auth }
    var items: any ItemsPort { dependency.items }
    var purchases: any PurchasesPort { dependency.purchases }
}
