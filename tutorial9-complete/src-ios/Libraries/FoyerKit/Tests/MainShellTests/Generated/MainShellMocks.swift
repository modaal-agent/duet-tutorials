// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:bffa67661ddf3bacdf2a981a96014776b9f0c1889d250bce9a5035bf6be0a408 template=Mocks.swifttemplate args=import=FoyerKit;import=MainShell
// input: sha256:74a64be299d44ad21259b76979e0a1669b69ebc02a0a86afa3a116616050db54 src-ios/Libraries/FoyerKit/Sources/MainShell/Generated/MainShellComponents.swift
// input: sha256:b963bfdb799b354d19bd95a339cfc812d2088882bb456a421fde197607a13ad2 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:21531b7f2cd0baef52b46b59ee2edc4c9697c9be26bd7870554a449bba3d3149 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:2af853b5b6524454a314671b90c6754c9e4b8036aaf57e4bdee8cecb74898e80 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:d372d4383643fecc3b63b2350b5cbfe51d9d27ccfcd138354f2690b934527033
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import MainShell

// MARK: - MainDependency
final class MainDependencyMock: MainDependency {

    // MARK: - Variables
    var account: any AccountPort
    var analytics: any AnalyticsTracking
    var auth: any AuthPort
    var items: any ItemsPort
    var purchases: any PurchasesPort

    // MARK: - Initializer
    init(account: any AccountPort, analytics: any AnalyticsTracking, auth: any AuthPort, items: any ItemsPort, purchases: any PurchasesPort) {
        self.account = account
        self.analytics = analytics
        self.auth = auth
        self.items = items
        self.purchases = purchases
    }
}
