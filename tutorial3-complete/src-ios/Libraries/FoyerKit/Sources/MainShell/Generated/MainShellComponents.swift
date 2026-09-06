// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:3ce3eb61d73896cda52c030daa0aee512bbda0fa5062874ce1e165f267c65304 template=Component.swifttemplate args=import=FoyerKit
// input: sha256:edf947f466adad7423d2b5785098a2aa629d8af57a1c2394554cd56b6a3498d9 src-ios/Libraries/FoyerKit/Sources/MainShell/MainBuilder.swift
// input: sha256:ba6c3c3dad2a82df2314539783e206d5bef1a71efbca259faadcf946fb1525e4 src-ios/Libraries/FoyerKit/Sources/MainShell/MainView.swift
// input: sha256:9657c254fc297c4fb63abff4f2abd8059f6eb39f78ba5f57ca394c05198b4040 src-ios/Libraries/FoyerKit/Sources/MainShell/MainViewShell.swift
// body: sha256:3da1d369a7898c998e2555460617c7f4fc216ea28ccb9bf2350db9d5b6356513
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
}
