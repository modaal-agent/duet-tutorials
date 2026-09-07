// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:bffa67661ddf3bacdf2a981a96014776b9f0c1889d250bce9a5035bf6be0a408 template=Mocks.swifttemplate args=import=FoyerKit;import=MainShell
// input: sha256:2a66ef24362b90f99f94ccb8933a1ab61eeb471ec05c5a482b4043a5a75c59bb src-ios/Libraries/FoyerKit/Sources/MainShell/Generated/MainShellComponents.swift
// input: sha256:3d0cf92f895fb2af9221bc12ea3ae696d0acefe83a5bcb0bacb7fd0fa4d16612 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:ba6c3c3dad2a82df2314539783e206d5bef1a71efbca259faadcf946fb1525e4 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:85a8ef5ca4957dad3bf9d6d27e6bc251bb8ce104c4c1439d04b8f92aaea2aac1 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:8a6a2cbf33e704d3a287ef58fcc513f40ced87d7ace5f9efd4cac9ccb649db7a
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import MainShell

// MARK: - MainDependency
final class MainDependencyMock: MainDependency {

    // MARK: - Variables
    var account: any AccountPort
    var auth: any AuthPort
    var items: any ItemsPort
    var purchases: any PurchasesPort

    // MARK: - Initializer
    init(account: any AccountPort, auth: any AuthPort, items: any ItemsPort, purchases: any PurchasesPort) {
        self.account = account
        self.auth = auth
        self.items = items
        self.purchases = purchases
    }
}
