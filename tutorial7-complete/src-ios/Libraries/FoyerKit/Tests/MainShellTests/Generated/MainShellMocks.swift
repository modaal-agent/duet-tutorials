// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:bffa67661ddf3bacdf2a981a96014776b9f0c1889d250bce9a5035bf6be0a408 template=Mocks.swifttemplate args=import=FoyerKit;import=MainShell
// input: sha256:3bf9eecf62ef63e65451b3b306c78e0cc7ab3a71e6d1e57eef0ff7de0e510d7a src-ios/Libraries/FoyerKit/Sources/MainShell/Generated/MainShellComponents.swift
// input: sha256:9d0b93179b055420a0e89a453cd6b013d73589accba3e80c0222fbcaf50f4ff2 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:9c9c82f95ffb065aeaeaadd23c4b15cb388f297251624a3e33520b297b612712 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:2af853b5b6524454a314671b90c6754c9e4b8036aaf57e4bdee8cecb74898e80 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
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
