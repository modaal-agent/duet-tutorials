// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:bffa67661ddf3bacdf2a981a96014776b9f0c1889d250bce9a5035bf6be0a408 template=Mocks.swifttemplate args=import=FoyerKit;import=MainShell
// input: sha256:daec1cd02fdec606a68cc0d1b7381e407904e4b40dc42df6ff73bab3725b223b src-ios/Libraries/FoyerKit/Sources/MainShell/Generated/MainShellComponents.swift
// input: sha256:edf947f466adad7423d2b5785098a2aa629d8af57a1c2394554cd56b6a3498d9 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:ba6c3c3dad2a82df2314539783e206d5bef1a71efbca259faadcf946fb1525e4 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:9657c254fc297c4fb63abff4f2abd8059f6eb39f78ba5f57ca394c05198b4040 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:44f477a2772c80979006fba54003b24e8e28415d17b255c8d944ea402da7fed2
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

    // MARK: - Initializer
    init(account: any AccountPort, auth: any AuthPort, items: any ItemsPort) {
        self.account = account
        self.auth = auth
        self.items = items
    }
}
