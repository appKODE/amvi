# Changelog

## 0.10.0 - 2026-11-08

* Kotlin Multiplatform: `jvm`, `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64` targets
* Update to Kotlin 2.3.20, coroutines 1.10.2. Minimum JVM version is 11
* Depend on unicorn `ru.kode:unicorn2:2.3.0` (new groupId, was `ru.dimsuz:unicorn2`)
* Add common `rememberViewIntents(factory)`. The reified no-arg `rememberViewIntents()` stays on JVM and Android
* `amvi-component-compose` depends only on Compose runtime (was Compose Desktop)
* Publish via Maven Central Portal

## 0.9.1 - 2023-02-15

* Fix bug with `ViewModel.attach` calls on a "leaked" view model object

## 0.9.0 - 2023-02-15

* Add `amvi-component-compose` module with an `MviComponent` composable function. It can be used in tandem with `ViewModel` where `ViewModel` directs component (screen) logic and `MviComponent` renders resulting `ViewState` changes

## 0.8.1 - 2022-10-27

* Initial release with a `ViewModel` class
