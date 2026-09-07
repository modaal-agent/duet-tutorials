// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:576e3a0d8869c20415a044a75d81de2af5ca35068cb09367655921472fdea4ba template=Mocks.swifttemplate args=import=FoyerKit;import=RootShell
// input: sha256:f042c1f1b93dcc7951f4c007e87b16d5e1bb5f6b1e47798369fa9cc599b38365 src-ios/Libraries/FoyerKit/Sources/RootShell/RootBuilder.swift
// input: sha256:3499a33982fd5e65f781fe0137224e51c5b04974d0f0b8f0fb2cb56df9c96b13 src-ios/Libraries/FoyerKit/Sources/RootShell/RootComposition.swift
// input: sha256:d5b5ea1604c12efa019b8edb4546129b14b6de3a12c30a0a2ccaa6ed88a2e88e src-ios/Libraries/FoyerKit/Sources/RootShell/RootView.swift
// input: sha256:4a42540b73d3869e6a8d0975d543ac2444ff0d5dacc10d62d5630a2c14c4faed src-ios/Libraries/FoyerKit/Sources/RootShell/RootViewShell.swift
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
