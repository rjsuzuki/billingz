# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Overview

Billingz is an Android library that wraps Google Play Billing and Amazon Appstore In-App Purchasing behind a single, vendor-agnostic API. Consumers integrate against `core` interfaces and a `BillingzStore.Builder` facade without referencing either platform SDK directly. Published to GitHub Packages and JitPack as three artifacts: `core`, `google`, `amazon`.

## Modules

The Gradle project (`settings.gradle`) builds `:lib:core`, `:lib:google`, `:lib:amazon`, and `:demo` (a Jetpack Compose sample app that exercises the library).

- **`lib/core`** — platform-agnostic interfaces and models only. No Google/Amazon SDK dependency. Exposes coroutines + LiveData/Flow APIs (`api`-scoped so consumers inherit them).
- **`lib/google`** — implements core against `com.android.billingclient:billing-ktx`. Depends on `:lib:core`.
- **`lib/amazon`** — implements core against `com.amazon.device:amazon-appstore-sdk`. Depends on `:lib:core`.

`google` and `amazon` are mutually exclusive: a consumer picks one. Both ship an identically-named `com.zuko.billingz.BillingzStore` entry point (the facade), so only one can be on the classpath at a time.

## Architecture

The design is Facade + Adapter. The `z`-suffixed names in `core` are the interfaces; platform modules provide the concrete adapters.

**Entry point flow:** `BillingzStore.Builder` (per-platform) → builds a `Storez` (`GoogleStore` / `AmazonStore`) → `Storez.getAgent()` returns an `Agentz`, the single facade developers call for all billing operations.

Core interface roles (`lib/core/.../store/`):
- **`Storez`** (`store/Storez.kt`) — the store instance + its `Builder`; extends `StoreLifecycle` (init/destroy tied to Android lifecycle).
- **`Agentz`** (`store/agent/Agentz.kt`) — the developer-facing facade: `startOrder`, `queryInventory`, `queryProduct`, `queryOrders`, `queryReceipts`, `getProduct(s)`, `completeOrder`, `cancelOrder`, plus readiness/connection state as both `LiveData` and `StateFlow`.
- **`Clientz`** (`store/client/Clientz.kt`) — connection lifecycle to the underlying billing service (`connect`/`disconnect`/`ConnectionStatus`).
- **`Salez`** (`store/sales/Salez.kt`) — purchase flow state machine: validate → process → complete/cancel/fail. Defines the two listeners consumers MUST implement: `OrderUpdaterListener` (purchase result callbacks) and `OrderValidatorListener` (server-side validation hook via `ValidatorCallback`).
- **`Inventoryz`** (`store/inventory/Inventoryz.kt`) — product catalog querying and caching.

Each platform module mirrors this structure under `store/{client,sales,inventory,model}/` (e.g. `GoogleStore`, `GoogleSales`, `GoogleInventory`, `GoogleClient` and their `Google*`/`Amazon*` model types adapting `Productz`, `Orderz`, `Receiptz`, `OrderHistoryz`, `QueryResult`).

When changing a billing capability, the change usually spans three layers: the `core` interface, then BOTH the `google` and `amazon` adapter implementations. Keep the two adapters behaviorally aligned.

## Build & Test

Uses the Gradle wrapper. Module-qualified tasks target a single library.

```bash
./gradlew build                          # build all modules
./gradlew :lib:google:assembleRelease    # build one module
./gradlew :lib:amazon:test               # JVM unit tests for a module
./gradlew :lib:amazon:test --tests "com.zuko.billingz.amazon.AmazonUnitTests"   # single test class
./gradlew :lib:amazon:connectedAndroidTest   # instrumented tests (needs device/emulator)
./gradlew lint                           # Android lint (abortOnError=true; baseline in lib*/lint-baseline.xml)
```

## Lint & Formatting

Spotless (`spotless.gradle`) enforces formatting: ktlint 0.46.0 for Kotlin, Google Java Format (AOSP) for Java, plus whitespace/newline rules on gradle/md/xml.

```bash
./gradlew spotlessApply    # auto-format
./gradlew spotlessCheck    # verify only
```

A `pre-commit` hook (`scripts/pre-commit`) runs `./gradlew lint spotlessApply` and blocks the commit on failure. Note: `gradle/tasks.gradle` installs a git hook from `scripts/pre-push` on `preBuild`, but the actual script in the repo is `scripts/pre-commit` — installation is a no-op until that mismatch is reconciled.

## Versioning & Publishing

- Version is `v.major.v.minor.v.patch` from `version.properties` (NOT from `LibraryInfo`); `gradle/version.gradle` reads it and computes artifact coordinates under group `com.zuko.billingz`.
- SDK/toolchain constants (compileSDK 34, minSDK 24, targetSDK 34, JVM/Java 17) live in `buildSrc/src/.../LibraryInfo.kt` and are referenced as `LibraryInfo.*` across all module build files. Change build-wide config there.
- Publishing (`gradle/publish.gradle`) targets GitHub Packages. Credentials come from env (`GPR_USER`, `GPR_API_KEY`) or a local `secrets.properties` (`gpr.usr` / `gpr.key`), which is auto-generated if absent.
- CI workflows live in `.github/workflows/` (release-package, create_release/prerelease, generate-docs, release_drafter).

## Conventions

- Public-facing core types use a trailing `z` (`Storez`, `Salez`, `Productz`, …); platform adapters are prefixed by vendor (`Google*`, `Amazon*`).
- API documentation is generated with Dokka (`./gradlew dokkaHtml`); keep KDoc current on `core` interfaces since they are the published contract.
