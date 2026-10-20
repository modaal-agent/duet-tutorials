// mock-templates:fingerprint v1
// bundle: 0.10.2
// config: sha256:f8c4b01ffe41a04cda6c79768281881158d128408241e0de3f0ae2cbc4386dea template=Mocks.swifttemplate args=import=Duet;import=SplashFeature
// input: sha256:d64e03b387abc053a5636f61addb5c558081b80c168cd7d28a83a9c674ad8bc9 src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashEnvironment.swift
// input: sha256:5a4efd106d5ba8a8201b4e14e3d624ebe995ad7d8338c9d4cf059e4374c56cf9 src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
// input: sha256:76d6a08d2723bb210e94e1fc1ecace58b63b1aea651d18ffbe543a9469375786 src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashRuntime.swift
// input: sha256:b9820bfff1b657874a8ba43d8d0f7c76c87165fd9706d6298a30958cdd490d23 src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashSerialization.generated.swift
// body: sha256:6db962b8740b68a25df5470b09b14a651fee5498ec5900668a167e4545fae056
// mock-templates:end
// Generated using Sourcery 2.3.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT


import Duet
import SplashFeature

// Mock member names are the requirement's declared name plus a suffix: `func load()` gives
// `loadCallCount`, `loadArgs` and `loadHandler`; `var name` gives `nameGetCount`, `nameGetHandler`,
// `nameSetCount`, `nameSetArgs`, `nameSetHandler` and the store `_name`. Where a prefix is not the declared name — an overload,
// a return-type discriminator, `sourcery: methodName` — a comment carrying both spellings
// sits above the witness and in the index under that class's `// MARK:` line.

// MARK: - SplashEnvironment
// Members are the requirement's declared name plus a suffix — `load` gives `loadCallCount`,
// `loadArgs`, `loadHandler`; `name` gives `nameGetCount`, `nameGetHandler`, `nameSetCount`, `nameSetArgs`, `nameSetHandler`, `_name`.
final class SplashEnvironmentMock: SplashEnvironment {

    // MARK: - Variables
    var clock: any KernelClock {
        clockGetCount += 1
        if let handler = clockGetHandler {
            return handler()
        }
        return _clock
    }
    var clockGetCount: Int = 0
    var clockGetHandler: (() -> any KernelClock)? = nil
    var _clock: any KernelClock

    // MARK: - Initializer
    init(clock: any KernelClock) {
        self._clock = clock
    }

    // MARK: - Methods
    func notifyHost(_ event: SplashDelegateEvent) {
        notifyHostCallCount += 1
        notifyHostArgs.append(event)
        if let __notifyHostHandler = self.notifyHostHandler {
            __notifyHostHandler(event)
        }
    }
    var notifyHostCallCount: Int = 0
    var notifyHostArgs: [SplashDelegateEvent] = []
    var notifyHostHandler: ((_ event: SplashDelegateEvent) -> ())? = nil
}
