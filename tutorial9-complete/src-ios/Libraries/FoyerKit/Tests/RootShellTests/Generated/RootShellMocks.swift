// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:576e3a0d8869c20415a044a75d81de2af5ca35068cb09367655921472fdea4ba template=Mocks.swifttemplate args=import=FoyerKit;import=RootShell
// input: sha256:adbaee94a50702e902f3b8818de183a49bf53592de6329e5b53bdb3923561839 src-ios/Libraries/FoyerKit/Sources/RootShell/ConsoleAnalyticsSink.swift
// input: sha256:754e6e764aef28be0885af3cc9da71ee6dc481380421559064551d83c6481d8b src-ios/Libraries/FoyerKit/Sources/RootShell/RootBuilder.swift
// input: sha256:ad0dbbe3f201fda1574aeeae0bfb7f3b18db70d3a70da90d93c89245f7c015b8 src-ios/Libraries/FoyerKit/Sources/RootShell/RootComposition.swift
// input: sha256:d5b5ea1604c12efa019b8edb4546129b14b6de3a12c30a0a2ccaa6ed88a2e88e src-ios/Libraries/FoyerKit/Sources/RootShell/RootView.swift
// input: sha256:046f38d4c724621ad691dcde3efda7a3361fd489c3b43f629f11335e324f9079 src-ios/Libraries/FoyerKit/Sources/RootShell/RootViewShell.swift
// input: sha256:b01372416d963f11d6f4da5d6bfcc3a78280f51ea3160ed946ee32f335fabd12 src-ios/Libraries/FoyerKit/Sources/RootShell/Workers.swift
// body: sha256:70c6256b01ce32504f001ba0a5a892ffbf21a960ffc436d5a51677b695820cca
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import FoyerKit
import RootShell

// MARK: - RootDependency
final class RootDependencyMock: RootDependency {

    // MARK: - Variables
    var storage: any KeyValueFile

    // MARK: - Initializer
    init(storage: any KeyValueFile) {
        self.storage = storage
    }
}
