// mock-templates:fingerprint v1
// bundle: 0.6.2
// config: sha256:576e3a0d8869c20415a044a75d81de2af5ca35068cb09367655921472fdea4ba template=Mocks.swifttemplate args=import=FoyerKit;import=RootShell
// input: sha256:d1a398d9312254db7da5f869e62e44c88dc7825d33981d6f16df532bf001db17 src-ios/Libraries/FoyerKit/Sources/RootShell/RootBuilder.swift
// input: sha256:3f16b09fee11d797bcd49eb28b5952ea5190902416ae98da4d302ba4d0e01d7e src-ios/Libraries/FoyerKit/Sources/RootShell/RootComposition.swift
// input: sha256:19428e1352cab94ae9b8d6ac7e52121e1b95df9af1eb2962a03148b21bf9593d src-ios/Libraries/FoyerKit/Sources/RootShell/RootView.swift
// input: sha256:9a90d3eae2dfd3037efe606a94b712700832ea1ec82654c55acf14827cdd437b src-ios/Libraries/FoyerKit/Sources/RootShell/RootViewShell.swift
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
