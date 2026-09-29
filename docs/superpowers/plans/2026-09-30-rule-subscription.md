# Official Rule Subscription Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship an official, signed GitHub rule subscription for OTP and verification-link wording, with application-only update prompts and a new signed APK release.

**Architecture:** Keep Android routing, candidate scoring, URL binding, and actions in the APK. Add a strict data-only rule pack, verify an Ed25519-signed manifest and the pack hash, then atomically activate an immutable snapshot. Check for updates when MainActivity enters the foreground; download and install only after a tap.

**Tech Stack:** Kotlin, Android API 36+, `org.json`, Java Security Ed25519, `HttpURLConnection`, Android `AtomicFile`, JUnit 4, Android instrumentation, JDK 21 source-file tool for signing.

**Spec:** `docs/superpowers/specs/2026-09-30-rule-subscription-design.md`

## Global Constraints

- Source: only `Venompool888/FluidCapsule` under `rules/stable/` on a fixed GitHub HTTPS host; no user supplied URLs or redirects.
- Rules are data only: OTP literals and two code-binding positions, verification-request literals, and additive exclusions. No arbitrary regex, code, whitelist changes, privacy changes, or URL-handler changes.
- `manifest.json` ≤ 8 KiB, pack ≤ 64 KiB, ≤ 200 rules, literal ≤ 120 characters; reject unknown JSON fields, duplicate IDs, invalid parameters, schema versions, and downgrades.
- Built-in rules remain available; current and previous verified remote packs may be retained. Subscription is on by default; checks happen only in the app, with a six-hour successful-check cache and a manual bypass.
- The user taps to install. No background worker, unlock receiver, system notification, automatic pack installation, third-party source, or APK self-update.
- Add `INTERNET` only for fixed GitHub GETs; no notification text, codes, URLs, device identifiers, history, or package inventories leave the device.
- Preserve current branch's uncommitted OTP and verification-link work. Real notification exports stay under ignored `runs/`; committed fixtures are synthetic.

## Review Focus

1. A broad remote OTP phrase beside an order number must not override built-in money/order/date/URL protections; Task 2 tests this.
2. A verification request beside an unrelated or second URL must not produce a direct-link action; Task 2 tests this.
3. A valid signed manifest paired with a different pack must leave the installed rules intact; Tasks 3 and 4 test this.
4. Corrupt current local storage must load the previous verified pack or built-in rules without crashing; Task 4 tests this.
5. Repeated foreground entries while offline or after “稍后” must not spam dialogs or install anything; Tasks 5 and 6 test this.

---

## File map and shared interfaces

In this plan, `rules/*.kt` under the app means `app/src/main/java/io/github/venompool888/fluidcapsule/rules/*.kt`; repository `rules/stable/` is the published data directory.

- `rules/RulePack.kt`: immutable rule and template types, plus `RulePack.EMPTY` version 0 and the no-additions bundled baseline `RulePack.BUNDLED` version 1.
- `rules/RulePackCodec.kt`: strict JSON decoder and all limits; no Android dependency.
- `rules/RuleManifest.kt`: strict manifest decoder and verified-manifest value.
- `rules/RuleTrust.kt`: Ed25519 and SHA-256 verification with an injectable public key; production loads the pinned DER asset.
- `rules/RuleStore.kt`: current/previous verified bundles in app-private files, atomic writes and recovery.
- `rules/RuleRuntime.kt`: process-wide immutable active snapshot for the listener.
- `rules/RuleHttpClient.kt`: fixed HTTPS GETs with byte/time limits, no redirects.
- `rules/RuleUpdateCoordinator.kt`: check/install state machine, version comparison and cache policy, with a small gateway interface and test factory for the UI.
- `rules/RuleSubscriptionPrefs.kt`: on/off, last successful check, reverified cached manifest/signature, dismissed version, restored version.
- `MainActivity.kt`: rule card, async foreground check and application-only dialog; existing card helpers stay in this file.
- `scripts/RulePackSigner.java`: local JDK 21 key-generation/signing CLI; private key outside Git.
- `rules/stable/`: signed initial manifest and pack. The APK's bundled baseline is version 1, so installing the new APK does not show an artificial update.

Shared signatures to preserve across tasks:

```kotlin
data class RulePack(val version: Int, val otpKeywords: List<LiteralRule>,
    val otpBindings: List<OtpBindingRule>, val verificationRequests: List<LiteralRule>,
    val otpExclusions: List<LiteralRule>, val linkExclusions: List<LiteralRule>) {
    companion object { val EMPTY: RulePack /* version 0 */; val BUNDLED: RulePack /* version 1 */ }
}
data class LiteralRule(val id: String, val phrase: String)
enum class CodePosition { BEFORE, AFTER }
data class OtpBindingRule(val id: String, val phrase: String,
    val codePosition: CodePosition, val maxDistance: Int)
object RulePackCodec { fun decode(bytes: ByteArray): RulePack }
object RuleRuntime {
    fun current(context: Context): RulePack
    fun activate(context: Context, verified: VerifiedRuleBundle)
    fun restoreBuiltIn(context: Context)
}
```

The JSON uses `schemaVersion`, `version`, `otpKeywords`, `otpBindings`, `verificationRequests`, `otpExclusions`, `linkExclusions`; each literal has `id` (`[a-z0-9._-]{1,64}`) and `phrase`, and each binding also has `codePosition` (`before`/`after`) and `maxDistance` (0..32). A bundle contains exact manifest, signature and pack bytes. Parser default parameters keep existing unit tests working; the notification listener explicitly passes `RuleRuntime.current(this)`.

### Task 1: Freeze and commit the existing OTP/verification-link baseline

**Files:** Existing modified parser, listener, action, publisher, test, README, architecture and `scripts/replay-verification-notifications.py` files shown by `git status --short`; no subscription files.

**Interfaces:** Produces the tested `OtpParser.parse(text)` and `VerificationLinkParser.parse(text)` baseline used by Task 2.

- [ ] **Step 1: Review** `git diff --check` and the existing synthetic tests for ordinary URL, misleading title, UNiDAYS, Riot and OTP-plus-link actions; keep private `runs/` untracked.
- [ ] **Step 2: Run the existing test cycle.** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`; expect `BUILD SUCCESSFUL`.
- [ ] **Step 3: Replay on the emulator.** Run `python3 scripts/replay-verification-notifications.py --serial emulator-5554`; expect all nine synthetic cases to match their expected `FILTERED`/`PUBLISHED` decisions.
- [ ] **Step 4: Commit only the baseline feature files.** Stage the paths listed in Step 1, inspect `git diff --cached --name-only`, then commit `feat: capture verification links and missed OTP formats`.

### Task 2: Strict rule-pack schema and typed parser integration

**Files:** Create `app/src/main/java/io/github/venompool888/fluidcapsule/rules/RulePack.kt`, `RulePackCodec.kt`; create `app/src/test/java/io/github/venompool888/fluidcapsule/rules/RulePackCodecTest.kt`; modify `parser/OtpParser.kt`, `parser/VerificationLinkParser.kt`, their tests.

**Interfaces:** Produce the shared `RulePack`/`RulePackCodec` declarations above. Change parser signatures to `OtpParser.parse(text: String, rules: RulePack = RulePack.EMPTY): OtpParseResult` and `VerificationLinkParser.parse(text: String, rules: RulePack = RulePack.EMPTY): VerificationLinkRequest?`.

- [ ] **Step 1: Write failing codec tests.** `validPackDecodesExactly` asserts one rule of each type and version 2; `rejectsUnknownFieldDuplicateIdAndOversize` asserts exceptions for an extra key, reused ID, a 121-character phrase, 201 rules, a 65,537-byte pack, and `maxDistance=33`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*RulePackCodecTest'`; expect failing tests because the codec is absent.
- [ ] **Step 3: Implement** the shared models and `RulePackCodec.decode(bytes)`, rejecting malformed UTF-8, unknown fields, invalid IDs, empty phrases and the Global Constraints limits. Do not compile downloaded regex.
- [ ] **Step 4: Write failing parser tests.** A pack with `secure number` plus `482913` in both code positions must capture; a pack with `order number` plus `482913` must stay rejected; a remote `Complete email confirmation` phrase with one adjacent URL must bind, while an unrelated footer URL or two candidate URLs must not bind.
- [ ] **Step 5: Run** `./gradlew :app:testDebugUnitTest --tests '*OtpParserTest' --tests '*VerificationLinkParserTest'`; expect the new cases to fail before integration.
- [ ] **Step 6: Integrate** remote literals into existing candidate scoring and verification-request matching. Apply built-in and remote exclusions before publishing a success; remote templates cannot bypass the current ambiguity, date, money, contact, order or URL checks. Keep default `EMPTY` behavior unchanged.
- [ ] **Step 7: Run** the codec and both parser suites plus `:app:testDebugUnitTest`; expect all passing. Commit `feat: add constrained subscription rule types`.

### Task 3: Signed manifest, official initial pack and publishing tool

**Files:** Create `rules/RuleManifest.kt`, `RuleTrust.kt`, their unit tests; create `scripts/RulePackSigner.java`, `rules/stable/manifest.json`, `rules/stable/manifest.sig`, `rules/stable/packs/1.json`, `rules/README.md`, and `app/src/main/assets/rule_signing_public.der`. Keep the PKCS#8 private key outside the repository.

**Interfaces:** `RuleManifestCodec.decode(bytes: ByteArray): RuleManifest`; `class RuleTrust(publicKey: PublicKey)` with `verifyManifest(bytes: ByteArray, base64Signature: ByteArray): VerifiedManifest` and `verifyPack(manifest: VerifiedManifest, packBytes: ByteArray): VerifiedRuleBundle`, plus `RuleTrust.official(context: Context)` to load the pinned asset. `RuleManifest` contains `schemaVersion`, `version`, `minAppVersionCode`, `packSha256`, `notes`. `VerifiedRuleBundle` exposes only verified bytes and decoded `RulePack`.

- [ ] **Step 1: Write failing trust tests.** A throwaway test key signs a manifest and matching pack; assertions cover success, one-byte manifest mutation, bad Base64 signature, wrong key, mismatched pack hash, pack-version mismatch, unknown field and 8,193-byte manifest.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*RuleTrustTest'`; expect failure before implementation.
- [ ] **Step 3: Implement** strict manifest decoding, Ed25519 verification against the bundled DER public key in production, and SHA-256 pack verification. Treat the downloaded signature as Base64 text; verify before comparing versions or displaying notes.
- [ ] **Step 4: Implement signer CLI** with `generate <private.pk8> <public.der>` and `sign <private.pk8> <manifest.json> <manifest.sig>` commands. Refuse to overwrite an existing private key; print no private bytes. Generate the official private key under `~/Library/Application Support/FluidCapsule/rules/` with mode 0600 and commit only the public key.
- [ ] **Step 5: Publish repository baseline** pack version 1 and a signed manifest with `minAppVersionCode=38`; pack 1 contains no remote additions, since the APK already includes the approved baseline behavior. `rules/README.md` documents review, signing, version increment and synthetic tests.
- [ ] **Step 6: Run** the trust suite and a test that validates the checked-in manifest/signature/pack with the checked-in public key; expect passing. Check `git status --short` contains no private key. Commit `feat: authenticate official rule packs`.

### Task 4: Atomic local storage and active snapshot

**Files:** Create `rules/RuleStore.kt`, `RuleRuntime.kt`; create `app/src/androidTest/java/io/github/venompool888/fluidcapsule/RuleStoreAndroidTest.kt`; modify `notification/CapsuleNotificationListenerService.kt` to pass the active pack to both parsers.

**Interfaces:** `RuleStore(context: Context, trust: RuleTrust)` exposes `loadLatest(): VerifiedRuleBundle?`, `install(bundle: VerifiedRuleBundle)`, and `restoreBuiltIn()`. `RuleRuntime` exposes the shared signatures above and treats built-in version 1 as current when no remote pack is active.

- [ ] **Step 1: Write failing instrumentation tests.** `installSurvivesRestart`, `corruptCurrentFallsBackToPrevious`, `corruptBothUsesBuiltIn`, and `incompatibleStoredPackUsesBuiltIn` assert the version and parser behavior after reloading from an app-private test directory. `restoreBuiltIn` must activate version 1 without deleting notification history.
- [ ] **Step 2: Run** `./gradlew :app:assembleDebugAndroidTest` and the targeted `RuleStoreAndroidTest` instrumentation on `emulator-5554`; expect failure before store implementation.
- [ ] **Step 3: Implement** atomic writes of complete verified bundles, retain current and previous copies, reverify bytes on load, and switch `RuleRuntime`'s immutable reference only after durable installation. Protect initialization and install from races; do not block notification parsing on network.
- [ ] **Step 4: Pass** `RuleRuntime.current(this)` into OTP and link parser calls in the notification listener. Run the targeted instrumentation and `:app:testDebugUnitTest`; expect passing. Commit `feat: activate verified rules atomically`.

### Task 5: Fixed-host checker and tap-to-install coordinator

**Files:** Create `rules/RuleHttpClient.kt`, `RuleUpdateCoordinator.kt`, `RuleSubscriptionPrefs.kt`, `app/src/test/java/io/github/venompool888/fluidcapsule/rules/RuleUpdateCoordinatorTest.kt`, `RuleHttpClientTest.kt`.

**Interfaces:** `RuleHttpClient.fetchManifest(): Pair<ByteArray, ByteArray>` and `fetchPack(version: Int): ByteArray`; production uses fixed `https://raw.githubusercontent.com/Venompool888/FluidCapsule/main/rules/stable/` paths. `RuleUpdateGateway` exposes `check(force: Boolean): RuleCheckResult`, `install(available: VerifiedManifest): RuleInstallResult`, `dismiss(version: Int)`, and `restoreBuiltIn()`; `RuleUpdateCoordinator` implements it. `RuleCheckResult.Available` contains the verified manifest and `shouldPrompt` boolean; other results distinguish up-to-date, incompatible, offline/failure. Inject client, trust, `installedVersion: () -> Int`, `activate: (VerifiedRuleBundle) -> Unit`, a `RuleUpdateStateStore` interface implemented by `RuleSubscriptionPrefs`, app `versionCode`, and clock for tests. The state store persists the last signed manifest bytes and signature; the coordinator re-verifies them before using the six-hour cache after process restart. An internal `RuleUpdateGatewayProvider.create(context)` factory allows instrumentation to replace the network gateway and restore it after each test.

- [ ] **Step 1: Write failing fake-client tests.** `sixHourCacheSkipsSecondNetworkCheck`, `cachedManifestSurvivesCoordinatorRestart`, `manualCheckBypassesCache`, `incompatibleManifestNeverOffersInstall`, `offlineKeepsCurrentVersion`, `tamperedPackDoesNotInstall`, `installRequiresUserCall`, and `dismissedVersionPromptsOnlyOnce`; use a fake client that counts calls and returns signed fixture bytes. In `RuleHttpClientTest`, a fake connection returning 302 or 65,537 pack bytes must fail without following the redirect or reading more bytes.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*RuleUpdateCoordinatorTest' --tests '*RuleHttpClientTest'`; expect failure before coordinator implementation.
- [ ] **Step 3: Implement** prefs and coordinator. A successful foreground check caches only the signed manifest for six hours, re-verifies it when read, and never downloads the pack; `install` fetches, verifies and activates the pack. No check runs when subscription is off unless `force=true`.
- [ ] **Step 4: Implement** the HTTP client with HTTPS, fixed path construction, redirects disabled, 5-second connect/read timeouts, GET only, 8 KiB manifest limit, 256-byte signature limit and 64 KiB pack limit. Inject a connection factory for tests while the production factory always uses the fixed URLs. Never include notification content or device identifiers in the URL, headers or logs.
- [ ] **Step 5: Run** the coordinator suite, `:app:testDebugUnitTest`, and a read-only manual GET of the checked-in official files; expect passing and verified baseline version 1. Commit `feat: check and install official rule updates`.

### Task 6: Rule settings card, application-only prompt and privacy copy

**Files:** Modify `MainActivity.kt:182,214`, `app/src/main/AndroidManifest.xml`, `README.md`, `docs/PRIVACY.md`, `docs/ARCHITECTURE.md`; create `app/src/androidTest/java/io/github/venompool888/fluidcapsule/RuleSubscriptionUiTest.kt`.

**Interfaces:** MainActivity obtains `RuleUpdateGateway` from `RuleUpdateGatewayProvider.create(this)`, calls `check(force=false)` on foreground entry using a dedicated executor, and posts results to the main thread. The Rules page offers subscription switch, installed/available version, last check status, manual check, update, and restore. One application dialog per new compatible version has `立即更新` and `稍后`.

- [ ] **Step 1: Write failing UI instrumentation tests** with `RuleUpdateGatewayProvider` set to a fake and reset after each test: opening Rules shows the default-on switch and version 1; available version 2 shows one dialog; after “稍后”, a second foreground entry shows none; “立即更新” calls `install` once; subscription off skips automatic check, while manual check still calls once.
- [ ] **Step 2: Run** targeted `RuleSubscriptionUiTest` instrumentation on `emulator-5554`; expect failure before wiring.
- [ ] **Step 3: Implement** the Rules card and nonblocking startup check. Update the UI on the main thread, persist dismissed/restored state through the gateway, show a concise offline/error status without a dialog, and never show a system notification.
- [ ] **Step 4: Add** `android.permission.INTERNET` and update English/Chinese privacy copy: fixed GitHub GETs only, no notification content uploaded, subscription default on and switchable. Remove every stale “no internet permission” statement.
- [ ] **Step 5: Run** targeted instrumentation, `:app:testDebugUnitTest`, `:app:lintDebug`, and `git diff --check`; expect passing. Commit `feat: add in-app rule subscription controls`.

### Task 7: End-to-end regression and signed release

**Files:** Modify `app/build.gradle.kts` for versionCode 38/versionName `1.2.0`; update `scripts/replay-verification-notifications.py` only if new fixtures are needed; add concise release notes under `docs/releases/1.2.0.md`.

**Interfaces:** No new runtime API; produces a signed APK and GitHub Release from the tested branch.

- [ ] **Step 1: Re-run** `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`; expect all passing. Run the nine synthetic emulator notifications and targeted UI/storage/action instrumentation; check ordinary links remain filtered and verification buttons still open the selected URL.
- [ ] **Step 2: Replay** all 5,126 private notification rows through both parser versions, save only aggregate comparisons to the ignored audit directory, and inspect every newly captured non-OTP row for false positives. Do not commit or publish private bodies, codes or links.
- [ ] **Step 3: Build** with `./scripts/build-release.sh`; expect signed `app/build/outputs/apk/release/app-release.apk`. Verify APK package, versionCode 38, versionName `1.2.0`, signature continuity and `INTERNET` permission before installation.
- [ ] **Step 4: Test** a cover-install on Pixel 11 Pro XL without clearing app data, confirm existing whitelist/history, check official version 1 reports up-to-date, and exercise an available-update flow with a signed test fixture on the emulator. Record any OEM promotion limit separately from classifier results.
- [ ] **Step 5: Commit** version and release notes, inspect the branch diff, push the branch, create and attach its pull request, and merge only after branch review and CI pass. Rebuild the signed APK from merged `main`, tag that commit `v1.2.0`, and publish its GitHub Release with the APK and notes. Confirm `rules/stable/` is reachable before release. Never attach or upload private audit files.

## Execution order

Tasks 1–7 are sequential because each publishes interfaces or verified artifacts used by the next. Review the changed files and relevant test output after each task; stop on any regression or signature/storage uncertainty and repair it before proceeding. The final whole-branch review should cover the security boundary, privacy claims, ordinary-link negatives and release artifact.
