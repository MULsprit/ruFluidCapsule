# Official rule subscription

`stable/manifest.json` and `stable/manifest.sig` are the signed update index. The manifest points to `stable/packs/<version>.json` by version and SHA-256. The app pins the Ed25519 public key in its APK and accepts only this repository's fixed GitHub path.

## Publishing a rule update

1. Review each new literal against synthetic OTP, verification-link, order, payment, date, phone-number, and ordinary-link cases. Keep rule packs data-only. Never add notification text or private audit exports to Git.
2. Increment both the pack and manifest `version`; keep `schemaVersion` at 1. Set `minAppVersionCode` to the oldest APK implementing the needed rule types. Keep JSON below the documented size and rule-count limits.
3. Calculate SHA-256 over the exact pack file bytes and put its lowercase hex digest in `packSha256`.
4. Sign the exact manifest bytes with `java scripts/RulePackSigner.java sign "$HOME/Library/Application Support/FluidCapsule/rules/official-private.pk8" rules/stable/manifest.json rules/stable/manifest.sig`. The private PKCS#8 file stays outside this repository with mode 0600. The `generate` command creates a new key pair only for a deliberate key rotation and refuses to overwrite an existing key.
5. Run `./gradlew :app:testDebugUnitTest` and the synthetic notification replay. Review the manifest, pack, signature, and test result before committing. Merge these files to the default branch together; the app checks that branch's fixed raw GitHub path.

The initial version 1 pack contains no remote additions. Its behavior is already bundled into app version 1.2.0. Algorithm, URL safety, and UI changes require a new APK.
